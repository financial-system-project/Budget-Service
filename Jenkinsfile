pipeline {
    agent any

    environment {
        IMAGE_NAME = "mahendra3/budget"
        IMAGE_TAG = "${BUILD_NUMBER}"
    }

    tools {
        maven 'maven_3.9'
    }

    stages{

        stage(' 1: cloning repository from GitHub') {
            steps {
                git branch: 'main' ,
                url: ' https://github.com/financial-system-project/Budget-Service.git '
            }
        }

        stage(' 2: Maven Build ') {
            steps {
                sh 'mvn clean install -DskipTests'
            }
        }

        stage(' 3: Unit Testing ') {
            steps {
                sh 'mvn test'
            }
        }

        stage(" 4: Building Docker Image ") {
            steps {
                script{
                    docker.build("${IMAGE_NAME}:${IMAGE_TAG}")
                }

            }
        }

        stage(" 5: Pushing Docker image to DockerHub") {

            steps {
                withCredentials([usernamePassword(credentialsId: 'DockerPwd',
                                  usernameVariable: 'USER',
                                  passwordVariable: 'PASS')]) {
                    sh '''
                        echo $PASS | docker login -u $USER --password-stdin
                        docker push ${IMAGE_NAME}:${IMAGE_TAG}
                    '''
                }
            }
        }

        stage('6: clean Docker Images') {
            steps {
                script {

                    // previous build number
                    def PREVIOUS_BUILD = env.BUILD_NUMBER.toInteger() - 1

                    // delete previous image if exists
                    sh """
                        docker rmi ${IMAGE_NAME}:${PREVIOUS_BUILD} || true
                        docker container prune -f
                        docker image prune -f
                    """
                }
            }
        }

        stage('7: Update GitOps Repo') {
            steps {

                withCredentials([string(
                    credentialsId: 'github-token',
                    variable: 'GITHUB_TOKEN'
                )]) {

                    sh """
                        rm -rf GitOps

                        git clone https://github.com/financial-system-project/GitOps.git

                        cd GitOps/charts/budget


                        sed -i '' '/repository: mahendra3\\/budget/{n;s/tag:.*/tag: "'${BUILD_NUMBER}'"/;}' values.yaml

                        git config user.email "jenkins@gmail.com"
                        git config user.name "jenkins"

                        git add values.yaml

                        git commit -m "Updated image tag to ${BUILD_NUMBER}"

                        git push https://\$GITHUB_TOKEN@github.com/financial-system-project/GitOps.git main
                    """
                }
            }
        }

        stage('8: Ansible Deployment') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'argocd-creds',
                    usernameVariable: 'ARGOCD_USER',
                    passwordVariable: 'ARGOCD_PASS')]) {
                    sh '''
                        export ARGOCD_USER=$ARGOCD_USER
                        export ARGOCD_PASS=$ARGOCD_PASS

                        cd GitOps

                        ansible-playbook Deployment/deploy.yaml -i Deployment/inventory.ini -e "argocd_app_name=budget-app"

                    '''
                }
            }
        }

    }
    post {
            always {
                mail(
                    to: 'mahendranath281201@gmail.com',
                    subject: "Build ${currentBuild.currentResult}",
                    body: "Build URL: ${env.BUILD_URL}"
                )
            }
        }

}