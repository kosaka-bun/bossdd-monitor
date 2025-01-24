chcp 65001

java -jar -Dfile.encoding=UTF-8 -Dspring.profiles.active=prod bossdd-monitor.jar ^
    --app.browser.default-headless=false ^
    --app.monitor.weekday-range=1-7 ^
    --app.monitor.hour-range=0-24
