pipeline {
    agent any

    environment {
        IMAGE_NAME = 'firepand4/fortress'
        DOCKER_CREDENTIALS = 'fba29f00-f5f0-4c9d-b8a5-74d8732276b4' // Set if you push to a private Docker registry
        BOT_TOKEN = credentials('budget-bot-token-id')
    }

    stages {
        stage('Get VCS') {
            steps {
                git branch: 'main', url: 'https://github.com/noobiland/telegram-budget-bot.git', credentialsId: '807ef3ed-90e8-440d-bff3-33bd0430c2d4'
            }
        }
        stage('Inject Resources') {
            steps {
                script {
                    // Define the path for the token file within the working directory
                    def filePath = "${pwd()}/resources"
                    def absFileName = "${filePath}/token"

                    // Ensure the directory exists
                    sh "mkdir -p ${filePath}"

                    // Create the file with the token
                    writeFile file: absFileName, text: BOT_TOKEN
                    echo "Token file created at ${absFileName}"
                }
            }
        }
        stage('Build docker image') {
            steps {
                sh('docker buildx build --platform linux/arm/v7 -t firepand4/fortress:budget-bot .')
            }
        }
        stage('Docker Login') {
            steps {
                withCredentials([usernamePassword(credentialsId: DOCKER_CREDENTIALS,
                                                 usernameVariable: 'DOCKER_USERNAME',
                                                 passwordVariable: 'DOCKER_PASSWORD')]) {
                    // Login to Docker registry
                    sh('echo $DOCKER_PASSWORD | docker login -u $DOCKER_USERNAME --password-stdin')
                                                 }
            }
        }
        stage('Push docker image') {
            steps {
                sh('docker push firepand4/fortress:budget-bot')
            }
        }
        stage('Docker stop container') {
            steps {
                sh('docker push firepand4/fortress:budget-bot')
            }
        }
        stage('Docker remove old container') {
            steps {
                sh('docker rm budget-bot-app')
            }
        }
        stage('Docker start new container') {
            steps {
                sh('docker run -dit --name budget-bot-app -v "/home/pi/temp/telegram:/output" firepand4/fortress:budget-bot')
            }
        }
    }
}
