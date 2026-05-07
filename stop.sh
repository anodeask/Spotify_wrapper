#!/bin/bash

set -u

BACKEND_PORT=9090
FRONTEND_PORT=3000
BACKEND_PATTERN='org.springframework.boot:spring-boot-maven-plugin:run|spring-boot'
FRONTEND_PATTERN='http.server 3000'

get_pids_by_port() {
    local port="$1"
    lsof -ti tcp:"$port" -sTCP:LISTEN 2>/dev/null | sort -u
}

get_pids_by_pattern() {
    local pattern="$1"
    pgrep -f "$pattern" 2>/dev/null | sort -u
}

merge_pids() {
    local port="$1"
    local pattern="$2"
    {
        get_pids_by_port "$port"
        get_pids_by_pattern "$pattern"
    } | awk 'NF { print }' | sort -u
}

print_service_status() {
    local name="$1"
    local port="$2"
    local pattern="$3"
    local pids

    pids="$(merge_pids "$port" "$pattern")"

    if [ -n "$pids" ]; then
        echo "$name: RUNNING"
        echo "  Port: $port"
        echo "  PIDs: $(echo "$pids" | tr '\n' ' ' | sed 's/[[:space:]]*$//')"
    else
        echo "$name: STOPPED"
        echo "  Port: $port"
    fi
}

kill_service() {
    local name="$1"
    local port="$2"
    local pattern="$3"
    local pids

    pids="$(merge_pids "$port" "$pattern")"

    if [ -z "$pids" ]; then
        echo "$name: no running process found"
        return 0
    fi

    echo "$name: stopping PIDs $(echo "$pids" | tr '\n' ' ' | sed 's/[[:space:]]*$//')"
    while IFS= read -r pid; do
        [ -n "$pid" ] && kill "$pid" 2>/dev/null || true
    done <<< "$pids"

    local remaining
    remaining="$(merge_pids "$port" "$pattern")"
    if [ -n "$remaining" ]; then
        echo "$name: force stopping remaining PIDs $(echo "$remaining" | tr '\n' ' ' | sed 's/[[:space:]]*$//')"
        while IFS= read -r pid; do
            [ -n "$pid" ] && kill -9 "$pid" 2>/dev/null || true
        done <<< "$remaining"
    fi
}

show_status() {
    echo "Checking Spotify Wrapper services..."
    print_service_status "Backend" "$BACKEND_PORT" "$BACKEND_PATTERN"
    print_service_status "Frontend" "$FRONTEND_PORT" "$FRONTEND_PATTERN"
}

stop_services() {
    echo "Stopping Spotify Wrapper services..."
    kill_service "Backend" "$BACKEND_PORT" "$BACKEND_PATTERN"
    kill_service "Frontend" "$FRONTEND_PORT" "$FRONTEND_PATTERN"
    echo
    show_status
}

case "${1:-stop}" in
    status|check)
        show_status
        ;;
    stop|kill)
        stop_services
        ;;
    *)
        echo "Usage: ./stop.sh [status|stop]"
        exit 1
        ;;
esac
