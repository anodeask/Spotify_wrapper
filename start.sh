#!/bin/bash

# Spotify Wrapper Application Startup Script

echo "🎵 Starting Spotify Wrapper Application..."

# Check if required directories exist
if [ ! -d "backend" ] || [ ! -d "frontend" ]; then
    echo "❌ Error: backend and frontend directories not found!"
    echo "Please run this script from the project root directory."
    exit 1
fi

# Function to check if a port is in use
check_port() {
    if lsof -i :$1 >/dev/null 2>&1; then
        echo "⚠️  Warning: Port $1 is already in use"
        return 1
    fi
    return 0
}

# Check required ports
echo "🔍 Checking ports..."
check_port 9090 || echo "   Backend might already be running"
check_port 3000 || echo "   Frontend might already be running"

# Start MySQL if not running (macOS with Homebrew)
if command -v brew >/dev/null 2>&1; then
    if ! brew services list | grep mysql | grep started >/dev/null 2>&1; then
        echo "🗄️  Starting MySQL..."
        brew services start mysql
        sleep 3
    else
        echo "✅ MySQL is already running"
    fi
fi

# Start backend
echo "🚀 Starting backend server..."
cd backend
if [ ! -f "pom.xml" ]; then
    echo "❌ Error: pom.xml not found in backend directory!"
    exit 1
fi

# Build and start backend in background
echo "   Compiling backend... (this may take a moment)"
mvn clean compile -q &
BACKEND_PID=$!

# Wait for compilation
wait $BACKEND_PID

if [ $? -ne 0 ]; then
    echo "❌ Error: Backend compilation failed!"
    exit 1
fi

echo "   Starting Spring Boot server..."

# Start backend server using full plugin path (more reliable)
mvn -f pom.xml org.springframework.boot:spring-boot-maven-plugin:run -DskipTests > ../backend/logs/backend-startup.log 2>&1 &
BACKEND_RUN_PID=$!

cd ..

# Start frontend
echo "🌐 Starting frontend server..."
cd frontend

if [ ! -f "index.html" ]; then
    echo "❌ Error: index.html not found in frontend directory!"
    kill $BACKEND_RUN_PID 2>/dev/null
    exit 1
fi

# Check if Python is available
if command -v python3 >/dev/null 2>&1; then
    PYTHON_CMD="python3"
elif command -v python >/dev/null 2>&1; then
    PYTHON_CMD="python"
else
    echo "❌ Error: Python is not installed!"
    kill $BACKEND_RUN_PID 2>/dev/null
    exit 1
fi

# Start frontend using Python HTTP server
echo "   Using $PYTHON_CMD to serve frontend on port 3000..."
$PYTHON_CMD -m http.server 3000 --bind 127.0.0.1 > ../frontend-startup.log 2>&1 &
FRONTEND_PID=$!

cd ..

# Wait for services to start
echo ""
echo "⏳ Waiting for services to start..."
sleep 5

# Check if backend is responding
echo "   Checking backend health..."
for i in {1..30}; do
    if curl -s http://127.0.0.1:9090/auth/login > /dev/null 2>&1; then
        echo "   ✅ Backend is ready!"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "   ⚠️  Backend may still be starting..."
    fi
    sleep 2
done

echo ""
echo "🎉 Application is starting up!"
echo ""
echo "📍 Services will be available at:"
echo "   Frontend: http://127.0.0.1:3000"
echo "   Backend:  http://127.0.0.1:9090"
echo ""
echo "🔧 Setup reminders:"
echo "   1. Make sure MySQL is running"
echo "   2. Update backend/src/main/resources/application.properties with your:"
echo "      - Database credentials"
echo "      - Spotify Client ID and Secret"
echo "   3. Add http://127.0.0.1:3000 to your Spotify app's redirect URIs"
echo ""
echo "📋 Log files:"
echo "   Backend:  backend/logs/backend-startup.log"
echo "   Frontend: frontend-startup.log"
echo ""
echo "🛑 To stop all services, press Ctrl+C or run: pkill -f 'spring-boot\\|http.server'"
echo ""

# Function to cleanup on script exit
cleanup() {
    echo ""
    echo "🛑 Shutting down services..."
    kill $BACKEND_RUN_PID $FRONTEND_PID 2>/dev/null
    echo "✅ Services stopped"
    exit 0
}

# Set trap to cleanup on script exit
trap cleanup INT TERM

# Wait for user to stop the script
echo "✅ Application is running! Press Ctrl+C to stop all services."
wait
