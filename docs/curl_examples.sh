# Example cURL Commands for API Testing (Updated)

# Always quote URLs in zsh to avoid globbing issues.

## Login as Team Lead (David Brown, id=4)
```
curl -sS -c cookies.txt -X POST "http://localhost:8080/api/auth/login" -H 'Content-Type: application/json' -d '{"username":"david.brown@company.com","password":"Teamlead@123"}' | cat
```

## Confirm Current User Info
```
curl -sS -b cookies.txt "http://localhost:8080/api/auth/me" | cat
```

## Employees (needed to derive team and unit membership)
# Get David Brown's employee record
```
curl -sS -b cookies.txt "http://localhost:8080/employees?page=0&size=100" | jq '.content[] | select(.email=="david.brown@company.com")'
```
# Get all employees in David's team (managerId == 1)
```
curl -sS -b cookies.txt "http://localhost:8080/employees?page=0&size=100" | jq '.content | map(select(.managerId==1)) | map(.firstName+" "+.lastName)'
```
# Get all employees in David's unit (unitId == 101)
```
curl -sS -b cookies.txt "http://localhost:8080/employees?page=0&size=200" | jq '.content | map(select(.unitId==101)) | map(.firstName+" "+.lastName)'
```

## Recognitions (fetch and inspect)
# Fetch a large page to allow client-side filters
```
curl -sS -b cookies.txt "http://localhost:8080/recognitions?page=0&size=1000" | jq '.content | length'
```

## Recognitions scoped to David (self)
# Using names because recognitions do not include senderEmail/recipientEmail
```
curl -sS -b cookies.txt "http://localhost:8080/recognitions?page=0&size=1000" | jq '.content | map(select(.senderName=="David Brown" or .recipientName=="David Brown")) | length'
```
# Preview a few
```
curl -sS -b cookies.txt "http://localhost:8080/recognitions?page=0&size=1000" | jq '.content | map(select(.senderName=="David Brown" or .recipientName=="David Brown")) | .[0:5]'
```

## Recognitions scoped to David's Team (managerId==1 -> list of names)
# First collect team member names
```
TEAM_NAMES=$(curl -sS -b cookies.txt "http://localhost:8080/employees?page=0&size=200" | jq -r '.content | map(select(.managerId==1)) | map(.firstName+" "+.lastName) | .[]')
```
# Then filter recognitions where sender or recipient is in TEAM_NAMES
```
curl -sS -b cookies.txt "http://localhost:8080/recognitions?page=0&size=1000" | jq --argjson names "$(printf '%s\n' $TEAM_NAMES | jq -R -s -c 'split("\n") | map(select(length>0))')" '.content | map(select((.senderName != null and (.senderName|tostring) as $s | $names|index($s)) or (.recipientName != null and (.recipientName|tostring) as $r | $names|index($r)))) | length'
```

## Recognitions scoped to David's Unit (unitId==101)
# Collect unit member names
```
UNIT_NAMES=$(curl -sS -b cookies.txt "http://localhost:8080/employees?page=0&size=200" | jq -r '.content | map(select(.unitId==101)) | map(.firstName+" "+.lastName) | .[]')
```
# Filter recognitions where sender or recipient is in UNIT_NAMES
```
curl -sS -b cookies.txt "http://localhost:8080/recognitions?page=0&size=1000" | jq --argjson names "$(printf '%s\n' $UNIT_NAMES | jq -R -s -c 'split("\n") | map(select(length>0))')" '.content | map(select((.senderName != null and (.senderName|tostring) as $s | $names|index($s)) or (.recipientName != null and (.recipientName|tostring) as $r | $names|index($r)))) | length'
```

## Leaderboard sanity checks
```
curl -sS -b cookies.txt "http://localhost:8080/leaderboard/top-recipients?page=0&size=5" | cat
curl -sS -b cookies.txt "http://localhost:8080/leaderboard/top-senders?page=0&size=5" | cat
```

# Notes:
# - Use quoted URLs in zsh to avoid globbing.
# - Recognitions API currently returns names and IDs, not emails; filters must use names or join via /employees.
# - Adjust managerId/unitId constants above if your environment differs.
