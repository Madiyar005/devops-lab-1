pipeline {
    agent any
    
    environment {
        // Docker образының аты
        DOCKER_IMAGE = "devops-app:${BUILD_NUMBER}"
        
        // Түстер (логтарды әдемілеу үшін)
        GREEN = '\u001b[32m'
        RED = '\u001b[31m'
        YELLOW = '\u001b[33m'
        BLUE = '\u001b[34m'
        RESET = '\u001b[0m'
    }
    
    stages {
        // ------------------------------------------------------------
        // 1-КЕЗЕҢ: GitHub-тан код алу
        // ------------------------------------------------------------
        stage('Checkout') {
            steps {
                script {
                    echo "${BLUE}========================================${RESET}"
                    echo "${BLUE}GitHub-тан код алу${RESET}"
                    echo "${BLUE}========================================${RESET}"
                }
                checkout scm
                echo "${GREEN}GitHub-тан код алынды${RESET}"
                
                // Қандай файлдар келгенін тексеру
                sh 'ls -la'
            }
        }
        
        // ------------------------------------------------------------
        // 2-КЕЗЕҢ: Java қолданбасын құрастыру (Maven)
        // ------------------------------------------------------------
        stage('Build Java App') {
            steps {
                script {
                    echo "${BLUE}========================================${RESET}"
                    echo "${BLUE}Java қолданбасын құрастыру${RESET}"
                    echo "${BLUE}========================================${RESET}"
                    
                    // Maven контейнерінде құрастыру
                    docker.image('maven:3.8-openjdk-11').inside {
                        sh '''
                            cd app
                            echo "Maven dependency-лерді жүктеу..."
                            mvn dependency:go-offline -q
                            
                            echo "Қолданбаны құрастыру..."
                            mvn clean package assembly:single -DskipTests
                            
                            echo "Құрастырылған файлдар:"
                            ls -la target/
                        '''
                    }
                }
                echo "${GREEN}Java қолданбасы құрастырылды${RESET}"
            }
        }
        
        // ------------------------------------------------------------
        // 3-КЕЗЕҢ: Docker образын құрастыру
        // ------------------------------------------------------------
        stage('Build Docker Image') {
            steps {
                script {
                    echo "${BLUE}========================================${RESET}"
                    echo "${BLUE}Docker образын құрастыру${RESET}"
                    echo "${BLUE}========================================${RESET}"
                    
                    // Docker образын құрастыру
                    sh """
                        echo "Образ аты: ${DOCKER_IMAGE}"
                        docker build -t ${DOCKER_IMAGE} .
                        docker tag ${DOCKER_IMAGE} devops-app:latest
                        echo "Образдар тізімі:"
                        docker images | head -5
                    """
                }
                echo "${GREEN}Docker образы құрастырылды: ${DOCKER_IMAGE}${RESET}"
            }
        }
        
        // ------------------------------------------------------------
        // 4-КЕЗЕҢ: Docker Compose сервистерін іске қосу
        // ------------------------------------------------------------
        stage('Start Services') {
            steps {
                script {
                    echo "${BLUE}========================================${RESET}"
                    echo "${BLUE}Docker Compose сервистерін іске қосу${RESET}"
                    echo "${BLUE}========================================${RESET}"
                    
                    // Ескі контейнерлерді тазалау
                    sh 'docker-compose down -v'
                    
                    // Жаңа сервистерді іске қосу
                    sh 'docker-compose up -d'
                    
                    // Сервистердің іске қосылуын күту
                    echo "Сервистердің іске қосылуын күту... (20 секунд)"
                    sleep 20
                    
                    // Жұмыс істеп тұрған контейнерлерді тексеру
                    sh 'docker-compose ps'
                }
                echo "${GREEN}Docker Compose сервистері іске қосылды${RESET}"
            }
        }
        
        // ------------------------------------------------------------
        // 5-КЕЗЕҢ: Redis тестілеу
        // ------------------------------------------------------------
        stage('Test Redis') {
            steps {
                script {
                    echo "${BLUE}========================================${RESET}"
                    echo "${BLUE}Redis тестілеу${RESET}"
                    echo "${BLUE}========================================${RESET}"
                    
                    // Redis счетчигін тексеру
                    sh '''
                        echo "Redis счетчигін тексеру..."
                        
                        # Бірнеше рет сұрау жіберу
                        for i in 1 2 3 4 5; do
                            echo "   Сұрау $i:"
                            curl -s http://localhost:5000/ | grep -E "hits|message" || echo "   Жауап жоқ"
                            sleep 1
                        done
                        
                        echo ""
                        echo "Redis ақпараты:"
                        curl -s http://localhost:5000/redis | python -m json.tool || echo "   Redis ақпараты жоқ"
                    '''
                }
                echo "${GREEN}Redis тестілеу аяқталды${RESET}"
            }
        }
        
        // ------------------------------------------------------------
        // 6-КЕЗЕҢ: Барлық сервистердің логтарын көрсету
        // ------------------------------------------------------------
        stage('Show Logs') {
            steps {
                script {
                    echo "${BLUE}========================================${RESET}"
                    echo "${BLUE}Сервистер логтары (соңғы 10 жол)${RESET}"
                    echo "${BLUE}========================================${RESET}"
                    
                    // Әр сервистің логтарын көрсету
                    sh '''
                        echo "APP логтары:"
                        docker-compose logs --tail=10 app || true
                        
                        echo ""
                        echo "REDIS логтары:"
                        docker-compose logs --tail=10 redis || true
                    '''
                }
            }
        }
    }
    
    // ------------------------------------------------------------
    // ПОСТ-ПРОЦЕССИНГ (әрқашан орындалады)
    // ------------------------------------------------------------
    post {
        always {
            script {
                echo "${YELLOW}========================================${RESET}"
                echo "${YELLOW}Тазалау жұмыстары${RESET}"
                echo "${YELLOW}========================================${RESET}"
                
                // Контейнерлерді тоқтату және жою
                sh 'docker-compose down -v'
                
                echo "${GREEN}Контейнерлер тазаланды${RESET}"
                echo "${YELLOW}========================================${RESET}"
            }
        }
        
        success {
            script {
                echo "${GREEN}========================================${RESET}"
                echo "${GREEN}ПАЙПЛАЙН СӘТТІ АЯҚТАЛДЫ!${RESET}"
                echo "${GREEN}========================================${RESET}"
                echo "Нәтижелер:"
                echo "   - Java қолданбасы құрастырылды"
                echo "   - Docker образы жасалды"
                echo "   - Redis тестіленді"
            }
        }
        
        failure {
            script {
                echo "${RED}========================================${RESET}"
                echo "${RED}ПАЙПЛАЙН ҚАТЕМЕН АЯҚТАЛДЫ!${RESET}"
                echo "${RED}========================================${RESET}"
                echo "Қате себептерін тексеру үшін:"
                echo "   - Консоль логтарын қараңыз"
                echo "   - docker-compose logs <сервис>"
                echo "   - Жеке сервистерді қолмен тексеріңіз"
            }
        }
    }
}
