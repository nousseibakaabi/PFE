@echo off
echo Building Spring Boot Backend...
cd back
docker build -t springboot-backend-app .
docker tag springboot-backend-app toutou11/springboot-backend-app:latest
docker push toutou11/springboot-backend-app:latest
cd ..
echo ✅ Backend deployed!
pause