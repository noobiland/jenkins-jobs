pipeline {
    agent any

    environment {
        IMAGE_NAME = 'firepand4/fortress:database-handler'
        CONTAINER_NAME = 'database-handler-app'
        DOCKER_CREDENTIALS = 'fba29f00-f5f0-4c9d-b8a5-74d8732276b4' // Set if you push to a private Docker registry
        USERS = 'users'
        VOLUME_SETTING = '/home/pi/Documents/databases:/databases'
        SECRET_FILE = credentials('users')
    }

    stages {
        stage('Clean workspace') {
            steps {
                script {
                    cleanWs()
                }
            }
        }
        stage('Get VCS') {
            steps {
                git branch: 'main', url: 'https://github.com/noobiland/database-handler.git', credentialsId: '807ef3ed-90e8-440d-bff3-33bd0430c2d4'
            }
        }
        stage('Inject Resources') {
            steps {
                script {
                    def data = readFile(file: SECRET_FILE)
                    writeFile(file: 'secret-data/users.csv', text: data)
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
        stage('Docker remove old container if exist') {
            steps {
                script {
                    sh """
                        if [ \$(docker ps -a -q -f name=\${CONTAINER_NAME}) ]; then
                            echo "Removing existing container: \${CONTAINER_NAME}"
                            docker rm -f \${CONTAINER_NAME}
                        else
                            echo "Container \${CONTAINER_NAME} does not exist, skipping removal."
                        fi
                    """
                }
            }
        }
    }
}
