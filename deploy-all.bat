@echo off
echo 🚀 DEPLOYING BACKEND + FRONTEND
echo.

echo Building Backend...
cd back && docker build -t springboot-backend-app . && docker tag springboot-backend-app toutou11/springboot-backend-app:latest && docker push toutou11/springboot-backend-app:latest
cd ..

echo.
echo Building Frontend...
cd front && docker build -t angular-frontend-app . && docker tag angular-frontend-app toutou11/angular-frontend-app:latest && docker push toutou11/angular-frontend-app:latest
cd ..

echo.
echo Restarting services...
docker-compose down
docker-compose up -d

echo.
echo ✅ DONE! Access at:
echo - Frontend: http://localhost:4200
echo - Backend:  http://localhost:8081
pause