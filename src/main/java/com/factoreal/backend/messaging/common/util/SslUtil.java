package com.factoreal.backend.messaging.common.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import javax.net.ssl.*;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * 🔐 AWS IoT MQTT 통신을 위한 SSLContext 생성 유틸리티 클래스
 * - 인증서 (device cert, private key, CA cert)를 이용하여 SSL 소켓을 생성함
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SslUtil {
    private final SecretsManagerClient awsSecretsManagerClient;

    /**
     * @param secretName       AWS SecretManager에서 사용한 secret 식별자
     * 🔐 PEM 텍스트 기반 SSLSocketFactory 생성 (Secrets Manager 등에서 가져온 경우)
     */
    public SSLSocketFactory getSocketFactoryFromSecrets(String secretName) throws Exception {
        // Step 1. SecretsManager에서 가져오기
        GetSecretValueRequest request = GetSecretValueRequest.builder()
                .secretId(secretName)
                .build();

        GetSecretValueResponse response = awsSecretsManagerClient.getSecretValue(request);

        if (response.secretString() == null) {
            throw new IllegalStateException("Secret is binary or null.");
        }

        // String의 경우 불변타입이여서 메모리에 남아 있음 -> Byte로 받아와서 비울 수 있도록
        byte[] secretBytes = response.secretString().getBytes(StandardCharsets.UTF_8);
        try {
            JsonNode json = new ObjectMapper().readTree(secretBytes);
            String deviceCert = json.get("certificate").asText();
            String privateKey = json.get("privateKey").asText();
            String rootCA = json.get("rootCA").asText();

            InputStream certStream = new ByteArrayInputStream(deviceCert.getBytes(StandardCharsets.UTF_8));
            InputStream keyStream = new ByteArrayInputStream(privateKey.getBytes(StandardCharsets.UTF_8));
            InputStream caStream = new ByteArrayInputStream(rootCA.getBytes(StandardCharsets.UTF_8));

            return createSocketFactory(caStream, certStream, keyStream);
        }catch (Exception e){
            log.info("❌Pem키 파싱 중 오류 발생");
            throw e;
        }finally {
            // 메모리에서 Secret Manager 정보 제거
            Arrays.fill(secretBytes, (byte) 0);
        }
    }

    /**
     * 📦 MQTT 연결용 SSLSocketFactory 생성 메서드
     * @param caCrtFile       AWS 루트 인증서 경로 (root.pem)
     * @param crtFile         디바이스 인증서 경로 (.pem.crt)
     * @param keyFile         디바이스 개인키 경로 (.pem.key)
     * @return SSLSocketFactory 객체
     * @throws Exception 모든 예외 전달 (파일, 키, 인증서 파싱 오류 등)
     */
    public SSLSocketFactory getSocketFactoryFromFiles(String caCrtFile, String crtFile, String keyFile) throws Exception {
        try (
                FileInputStream caFis = new FileInputStream(caCrtFile);
                FileInputStream crtFis = new FileInputStream(crtFile);
                FileInputStream keyReader = new FileInputStream(keyFile)
        ) {
            return createSocketFactory(caFis, crtFis, keyReader);
        }
    }

    private static SSLSocketFactory createSocketFactory(InputStream caFile, InputStream certInput, InputStream keyInput) throws Exception {
        // BouncyCastle Provider 등록 (PEM 파싱용)
        Security.addProvider(new BouncyCastleProvider());

        // CA 인증서와 디바이스 인증서
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate caCert = (X509Certificate) cf.generateCertificate(caFile);
        X509Certificate cert = (X509Certificate) cf.generateCertificate(certInput);

        // 디바이스 개인키 PEM → Keypair 변환
        PEMParser pemParser = new PEMParser(new InputStreamReader(keyInput,StandardCharsets.UTF_8));
        Object object = pemParser.readObject();
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
        KeyPair key = converter.getKeyPair((PEMKeyPair) object);
        pemParser.close();

        // 키스토어 구성 (디바이스 인증서 + 개인키)
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, null);
        ks.setCertificateEntry("cert-alias", cert);
        ks.setKeyEntry("key-alias", key.getPrivate(), "".toCharArray(), new Certificate[]{cert});

        // 트러스트스토어 구성 (루트 CA 인증서)
        KeyStore ts = KeyStore.getInstance(KeyStore.getDefaultType());
        ts.load(null, null);
        ts.setCertificateEntry("ca-alias", caCert);


        // [추가] JVM 기본 TrustManager (기본 TrustStore 포함)
        TrustManagerFactory jvmTmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        jvmTmf.init((KeyStore) null); // ← 기본 truststore 사용


        // ✅ [기존] root.pem 기반 TrustManager
        TrustManagerFactory customTmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        customTmf.init(ts);


        // ✅ [추가] TrustManager 병합 (기본 + root.pem)
        TrustManager[] mergedTrustManagers = Stream
                .concat(Arrays.stream(jvmTmf.getTrustManagers()), Arrays.stream(customTmf.getTrustManagers()))
                .toArray(TrustManager[]::new);


        // 5. KeyManagerFactory 구성
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, "".toCharArray());

        SSLContext context = SSLContext.getInstance("TLSv1.2");
        context.init(kmf.getKeyManagers(), mergedTrustManagers, null); // ✅ [변경] 병합한 TrustManager 적용

        return context.getSocketFactory();
    }
}