pipeline {
  agent any

  environment {
    /* AWS & ECR */
    AWS_DEFAULT_REGION = 'ap-northeast-2'
    ECR_REGISTRY       = '853660505909.dkr.ecr.ap-northeast-2.amazonaws.com'
    IMAGE_REPO_NAME    = 'springboot'
    IMAGE_TAG          = "${env.GIT_COMMIT}"
    LATEST_TAG         = 'backend-latest'

    /* GitHub Checks */
    GH_CHECK_NAME      = 'BE Build Test'
  }

  stages {
    /* 1) 공통 테스트 */
    stage('Test') {
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

./gradlew test --no-daemon
'''
        }
      }
      post {
        success {
          publishChecks name: GH_CHECK_NAME,
                        conclusion: 'SUCCESS',
                        detailsURL: env.BUILD_URL
        }
        failure {
          publishChecks name: GH_CHECK_NAME,
                        conclusion: 'FAILURE',
                        detailsURL: "${env.BUILD_URL}console"
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

docker build -t ${ECR_REGISTRY}/${IMAGE_REPO_NAME}:${LATEST_TAG} .

docker push ${ECR_REGISTRY}/${IMAGE_REPO_NAME}:${LATEST_TAG}
          """
        }
      }
    }

    /* 3) develop 전용 ─ EC2 배포 */
    stage('Deploy to EC2 (develop only)') {
      when {
        allOf {
          branch 'develop'
          not { changeRequest() } // PR 빌드는 건너뜀
        }
      }
      steps {
        sshagent(credentials: ['monitory-temp']) {
          sh '''
ssh -o StrictHostKeyChecking=no ec2-user@43.200.39.139 <<'EOF'
set -e
cd datastream
docker-compose -f docker-compose-service.yml down -v
docker-compose -f docker-compose-service.yml up -d --pull always --build
EOF
'''
        }
      }
    }
  }
  /* TODO. Slack */
}