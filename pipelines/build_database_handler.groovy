pipeline {
    agent any

    environment {
        IMAGE_NAME = 'firepand4/fortress:database-handler'
        CONTAINER_NAME = 'database-handler-app'
        DOCKER_CREDENTIALS = 'fba29f00-f5f0-4c9d-b8a5-74d8732276b4' // Set if you push to a private Docker registry
        USERS = credentials('users')
        VOLUME_SETTING = '/home/pi/Documents/databases:/databases'
    }

    stages {
        stage('Get VCS') {
            steps {
                git branch: 'main', url: 'https://github.com/noobiland/database-handler.git', credentialsId: '807ef3ed-90e8-440d-bff3-33bd0430c2d4'
            }
        }
        stage('Inject Resources') {
            steps {
                withCredentials([file(credentialsId: USERS, variable: 'SECRET_FILE')]) {
                    def filePath = "${pwd()}/resources"
                    sh("cp $SECRET_FILE $filePath")
                }
            }
        }
        stage('Build docker image') {
            steps {
                sh("docker buildx build --platform linux/arm/v7 -t $IMAGE_NAME .")
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
                sh("docker push $IMAGE_NAME")
            }
        }
        stage('Docker stop container') {
            steps {
                sh("docker push $IMAGE_NAME")
            }
        }
        stage('Docker remove old container') {
            steps {
                sh("docker rm $CONTAINER_NAME")
            }
        }
        stage('Docker start new container') {
            steps {
                sh("docker run -dit --name $CONTAINER_NAME -v '$VOLUME_SETTING' $IMAGE_NAME")
            }
        }
    }
}
