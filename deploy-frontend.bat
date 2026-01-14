@echo off
echo Building Angular Frontend...
cd front
docker build -t angular-frontend-app .
docker tag angular-frontend-app toutou11/angular-frontend-app:latest
docker push toutou11/angular-frontend-app:latest
cd ..
echo ✅ Frontend deployed!
pause