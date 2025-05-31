package com.factoreal.backend.global.exception.application;

import com.slack.api.Slack;
import com.slack.api.model.block.Blocks;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.composition.BlockCompositions;
import com.slack.api.webhook.WebhookPayloads;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import static com.slack.api.model.block.composition.BlockCompositions.plainText;

@Service
@Slf4j
@Profile("cloud") // cloud profile에서만 활성화
public class SlackService {
    private static final String NEW_LINE = "\n";
    @Value("${webhook.slack.url}")
    private String SLACK_WEBHOOK_URL; // 채널마다 or 사용자마다 하나씩 발급받아야함

    private final Slack slackClient = Slack.getInstance();

    private final StringBuilder sb= new StringBuilder();

    // Slack으로 알림 보내기
    public void sendAlert(Exception error, HttpServletRequest request) throws IOException {

        // 현재 프로파일이 특정 프로파일이 아니면 알림보내지 않기
//        if (!env.getActiveProfiles()[0].equals("set1")) {
//            return;
//        }


        // 메시지 내용인 LayoutBlock List 생성
        List<LayoutBlock> layoutBlocks= generateLayoutBlock(error,request);

        // 슬랙의 send API과 webhookURL을 통해 생성한 메시지 내용 전송
        slackClient.send(SLACK_WEBHOOK_URL, WebhookPayloads
            .payload(p->
                // 메시지 내용
                p.blocks(layoutBlocks)
            )
        );
    }

    // 전체 메시지가 담긴 LayoutBlock 생성
    private List<LayoutBlock> generateLayoutBlock(Exception error, HttpServletRequest request) {
        return Blocks.asBlocks(
            getHeader("서버 측 오류로 예상되는 예외 상황이 발생하였습니다."),
            Blocks.divider(),
            getSection(generateErrorMessage(error)),
            Blocks.divider(),
            getSection(generateErrorPointMessage(request))
//            Blocks.divider(),
            // 이슈 생성을 위해 프로젝트의 Issue URL을 입력하여 바로가기 링크를 생성
//            getSection("<github_issue_url)|이슈 생성하러=\"\" 가기=\"\">")
        );
    }

    // 예외 정보 메시지 생성
    private String generateErrorMessage(Exception error) {
        sb.setLength(0);
        sb.append("*[🔥 Exception]*").append(NEW_LINE).append(error.toString()).append(NEW_LINE).append(NEW_LINE);
        sb.append("*[📩 From]*").append(NEW_LINE).append(readRootStackTrace(error)).append(NEW_LINE).append(NEW_LINE);

        return sb.toString();
    }

    // HttpServletRequest를 사용하여 예외발생 요청에 대한 정보 메시지 생성
    private String generateErrorPointMessage(HttpServletRequest request) {
        sb.setLength(0);
        sb.append("*[🧾세부정보]*").append(NEW_LINE);
        sb.append("Request URL : ").append(request.getRequestURL().toString()).append(NEW_LINE);
        sb.append("Request Method : ").append(request.getMethod()).append(NEW_LINE);
        sb.append("Request Time : ").append(new Date()).append(NEW_LINE);

        return sb.toString();
    }

    // 예외발생 클래스 정보 return
    private String readRootStackTrace(Exception error) {
        return error.getStackTrace()[0].toString();
    }
    // 에러 로그 메시지의 제목 return
    private LayoutBlock getHeader(String text) {
        return Blocks.header(h->h.text(
            plainText(pt->pt.emoji(true)
                .text(text))));
    }

    // 에러 로그 메시지 내용 return
    private LayoutBlock getSection(String message) {
        return Blocks.section(s->
            s.text(BlockCompositions.markdownText(message)));
    }
}
