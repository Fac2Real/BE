pipeline {
  agent any

  environment {
    /* AWS & ECR */
    AWS_DEFAULT_REGION = 'ap-northeast-2'
    ECR_REGISTRY       = '853660505909.dkr.ecr.ap-northeast-2.amazonaws.com'
    IMAGE_REPO_NAME    = 'springboot'
    IMAGE_TAG          = "${env.GIT_COMMIT}"
    LATEST_TAG         = 'backend-latest'
    PROD_TAG           = 'backend-prod-latest'

    /* GitHub Checks */
    GH_CHECK_NAME      = 'BE Build Test'

    /* Slack */
    SLACK_CHANNEL      = '#ci-cd'
    SLACK_CRED_ID      = 'slack-factoreal-token'   // Slack App OAuth Token

    /* Argo CD */
    HELM_VALUES_PATH        = 'monitory-helm-charts/backend/values.yaml'
    ARGOCD_SERVER           = 'argocd.monitory.space'   // Argo CD server endpoint
    ARGOCD_APPLICATION_NAME = 'backend'
  }

  stages {
    /* 0) 환경 변수 설정 */
    stage('Environment Setup') {
      steps {
        script {
          def rawUrl = sh(script: "git config --get remote.origin.url",
                        returnStdout: true).trim()
          env.REPO_URL = rawUrl.replaceAll(/\.git$/, '')
          env.COMMIT_MSG = sh(script: "git log -1 --pretty=format:'%s'",returnStdout: true).trim()
        }
      }
    }

    // ✅ PR 전용 테스트 단계
    stage('Test for PR') {
      when {
        changeRequest()
      }
      steps {
        withCredentials([file(credentialsId: 'backend-env', variable: 'ENV_FILE')]) {
          sh '''
set -o allexport
source "$ENV_FILE"
set +o allexport

./gradlew test jacocoTestReport --no-daemon
'''
        }
      }
      post {
        success {
          archiveArtifacts artifacts: 'build/reports/jacoco/test/**', fingerprint: true
        }
      }
    }

    // ✅ PR이 아닐 때만 실행되는 테스트 (develop/main 제외)
    stage('Test') {
      when {
        allOf {
          not { branch 'develop' }
          not { branch 'main' }
          not { changeRequest() }
        }
      }
      steps {
        publishChecks name: GH_CHECK_NAME,
                      status: 'IN_PROGRESS',
                      detailsURL: env.BUILD_URL

        // Gradle 빌드 환경 변수 설정
        withCredentials([ file(credentialsId: 'backend-env', variable: 'ENV_FILE') ]) {
        sh '''
set -o allexport
source "$ENV_FILE"
set +o allexport

./gradlew test jacocoTestReport --no-daemon
'''
        }
      }
      post {
        success {
          publishChecks name: GH_CHECK_NAME,
                        conclusion: 'SUCCESS',
                        detailsURL: env.BUILD_URL

          jacoco execPattern: 'build/jacoco/test.exec',
                 classPattern: 'build/classes/java/main',
                 sourcePattern: 'src/main/java',
                 inclusionPattern: '**/*.class',
                 exclusionPattern: '**/*Test*',
                 changeBuildStatus: true

          archiveArtifacts artifacts: 'build/reports/jacoco/test/html/**', fingerprint: true
        }
        failure {
          publishChecks name: GH_CHECK_NAME,
                        conclusion: 'FAILURE',
                        detailsURL: "${env.BUILD_URL}console"
          slackSend channel: env.SLACK_CHANNEL,
                              tokenCredentialId: env.SLACK_CRED_ID,
                              color: '#ff0000',
                              message: """:x: *BE Test 실패*
파이프라인: <${env.BUILD_URL}|열기>
커밋: `${env.GIT_COMMIT}` – `${env.COMMIT_MSG}`
(<${env.REPO_URL}/commit/${env.GIT_COMMIT}|커밋 보기>)
"""
        }
      }
    }



    /* 2) develop 전용 ─ Docker 이미지 빌드 & ECR Push */
    stage('Docker Build & Push (develop only)') {
      when {
        allOf {
          branch 'develop'
          not { changeRequest() }
        }
      }
      steps {
        withCredentials([[$class: 'AmazonWebServicesCredentialsBinding',
                          credentialsId: 'jenkins-access']]) {
          sh """
aws ecr get-login-password --region ${AWS_DEFAULT_REGION} \
  | docker login --username AWS --password-stdin ${ECR_REGISTRY}

docker build -t ${ECR_REGISTRY}/${IMAGE_REPO_NAME}:${LATEST_TAG} -t ${ECR_REGISTRY}/${IMAGE_REPO_NAME}:${env.GIT_COMMIT} .

docker push ${ECR_REGISTRY}/${IMAGE_REPO_NAME}:${LATEST_TAG}
docker push ${ECR_REGISTRY}/${IMAGE_REPO_NAME}:${env.GIT_COMMIT}
"""
        }
      }
      /* Slack 알림 */
      post {
        failure {
          slackSend channel: env.SLACK_CHANNEL,
                    tokenCredentialId: env.SLACK_CRED_ID,
                    color: '#ff0000',
                    message: """:x: *BE develop branch CI 실패*
파이프라인: <${env.BUILD_URL}|열기>
커밋: `${env.GIT_COMMIT}` – `${env.COMMIT_MSG}`
(<${env.REPO_URL}/commit/${env.GIT_COMMIT}|커밋 보기>)
"""
        }
      }
    }

    /* 3) develop 전용 ─ Argo CD 배포 */
    stage('Deploy (develop only)') {
      when {
        allOf {
          branch 'develop'
          not { changeRequest() }
        }
      }
      steps {
        withCredentials([string(credentialsId: 'monitory-iac-github-token', variable: 'GIT_TOKEN')]){
          sh """
git clone https://${GIT_TOKEN}@github.com/Fac2Real/monitory-iac.git ${env.GIT_COMMIT}
cd ${env.GIT_COMMIT}
git checkout deploy
git fetch origin main
git merge --no-ff origin/main -m "ci: merge main → deploy"
yq -i ".image.tag = \\"${env.GIT_COMMIT}\\"" ${HELM_VALUES_PATH}
git config user.name  "ci-bot"
git config user.email "ci-bot@monitory.space"
git add ${HELM_VALUES_PATH}
git commit -m "ci(${ARGOCD_APPLICATION_NAME}): bump image to ${env.GIT_COMMIT})"
git push https://${GIT_TOKEN}@github.com/Fac2Real/monitory-iac.git deploy
"""
        }

        withCredentials([string(credentialsId: 'argo-jenkins-token', variable: 'ARGOCD_AUTH_TOKEN')]) {
          sh '''
argocd --server $ARGOCD_SERVER --insecure --grpc-web \
        app sync $ARGOCD_APPLICATION_NAME

argocd --server $ARGOCD_SERVER --insecure --grpc-web \
        app wait $ARGOCD_APPLICATION_NAME --health --timeout 300
'''
        }
      }
      /* Slack 알림 */
      post {
        success {
          slackSend channel: env.SLACK_CHANNEL,
                    tokenCredentialId: env.SLACK_CRED_ID,
                    color: '#36a64f',
                    message: """:white_check_mark: *BE develop branch CI/CD 성공*
파이프라인: <${env.BUILD_URL}|열기>
커밋: `${env.GIT_COMMIT}` – `${env.COMMIT_MSG}`
(<${env.REPO_URL}/commit/${env.GIT_COMMIT}|커밋 보기>) (<https://argocd.monitory.space/applications/argocd/backend|Argo CD 보기>)
"""
        }
        failure {
          slackSend channel: env.SLACK_CHANNEL,
                    tokenCredentialId: env.SLACK_CRED_ID,
                    color: '#ff0000',
                    message: """:x: *BE develop branch CD 실패*
파이프라인: <${env.BUILD_URL}|열기>
커밋: `${env.GIT_COMMIT}` – `${env.COMMIT_MSG}`
(<${env.REPO_URL}/commit/${env.GIT_COMMIT}|커밋 보기>) (<https://argocd.monitory.space/applications/argocd/backend|Argo CD 보기>)
"""
        }
      }
    }
    stage('Check Jacoco CSV File') {
            steps {
                sh 'ls -l build/reports/jacoco/test/'
            }
        }
    /* 4) PR 코맨트에 ─ 커버리지 테스트 요약 작성 */
    stage('Report Coverage to PR') {
      when {
        changeRequest()
      }
      steps {
        script {
          def coverageFile = readFile('build/reports/jacoco/test/jacocoTestReport.csv')
          def lineCoverage = coverageFile.readLines().find { it.contains('LINE') }
          def branchCoverage = coverageFile.readLines().find { it.contains('BRANCH') }

          def coverageSummary = """
    ### ✅ Test Coverage Report

    - **Line**: ${lineCoverage}
    - **Branch**: ${branchCoverage}

    [View Full Report](${env.BUILD_URL}artifact/build/reports/jacoco/test/html/index.html)
    """

          // GitHub API로 PR 코멘트 작성
          sh """
    curl -s -H "Authorization: token ${GITHUB_TOKEN}" \\
         -X POST -d '{ "body": """${coverageSummary.replace("\"", "\\\"")}""" }' \\
         https://api.github.com/repos/${env.REPO_NAME}/issues/${env.CHANGE_ID}/comments
    """
        }
      }
    }



    /* 4) main 전용 ─ 이미지 빌드 & ECR Push (EC2) */
    stage('Docker Build & Push (main only)') {
      when {
        allOf {
          branch 'main'
          not { changeRequest() }
        }
      }
      steps {
        withCredentials([[$class: 'AmazonWebServicesCredentialsBinding',
                          credentialsId: 'jenkins-access']]) {
          sh """
aws ecr get-login-password --region ${AWS_DEFAULT_REGION} \
  | docker login --username AWS --password-stdin ${ECR_REGISTRY}

docker build -t ${ECR_REGISTRY}/${IMAGE_REPO_NAME}:${PROD_TAG} -t ${ECR_REGISTRY}/${IMAGE_REPO_NAME}:${env.GIT_COMMIT} .

docker push ${ECR_REGISTRY}/${IMAGE_REPO_NAME}:${PROD_TAG}
docker push ${ECR_REGISTRY}/${IMAGE_REPO_NAME}:${env.GIT_COMMIT}
"""
        }
      }
      /* Slack 알림 */
      post {
        success {
          slackSend channel: env.SLACK_CHANNEL,
                    tokenCredentialId: env.SLACK_CRED_ID,
                    color: '#36a64f',
                    message: """:white_check_mark: *BE main branch CI 성공*
파이프라인: <${env.BUILD_URL}|열기>
커밋: `${env.GIT_COMMIT}` – `${env.COMMIT_MSG}`
(<${env.REPO_URL}/commit/${env.GIT_COMMIT}|커밋 보기>)
"""
        }
        failure {
          slackSend channel: env.SLACK_CHANNEL,
                    tokenCredentialId: env.SLACK_CRED_ID,
                    color: '#ff0000',
                    message: """:x: *BE main branch CI 실패*
파이프라인: <${env.BUILD_URL}|열기>
커밋: `${env.GIT_COMMIT}` – `${env.COMMIT_MSG}`
(<${env.REPO_URL}/commit/${env.GIT_COMMIT}|커밋 보기>)
"""
        }
      }
    }

  }
}
