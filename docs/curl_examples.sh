#!/bin/bash
# Curl examples for all endpoints in endpoints.md
# Update BASE_URL as needed
BASE_URL="http://localhost:8080"

# Array to store results
results=()

# Generate random UUID and timestamp for test data
function rand_uuid {
  cat /proc/sys/kernel/random/uuid 2>/dev/null || uuidgen
}
function rand_email {
  echo "user$RANDOM@example.com"
}
function now_iso {
  date -u +"%Y-%m-%dT%H:%M:%SZ"
}

# Function to execute curl command and capture output and HTTP status
function curl_command {
  local method="$1"
  local url="$2"
  local data="$3"
  local content_type="$4"
  local description="$5"

  response=$(curl -s -w "%{http_code}" -X "$method" "$url" -H "Content-Type: $content_type" -d "$data")
  http_status="${response: -3}"
  response_body="${response:0:${#response}-3}"

  # Print response body for debugging
  echo "Response Body: $response_body"

  # Check HTTP status and print pass/fail
  if [[ "$http_status" -ge 200 && "$http_status" -lt 300 ]]; then
    echo "PASS: $http_status"
    results+=("PASS: $description ($http_status)")
  else
    echo "FAIL: $http_status"
    results+=("FAIL: $description ($http_status)")
  fi
  # Return response body for chaining
  echo "$response_body"
}

# 1. Recognition Management

recognitionTypeUuid=$(rand_uuid)
recipientUuid=$(rand_uuid)
senderUuid=$(rand_uuid)
awardName="Employee of the Month $RANDOM"
level="Gold"
message="Great job $RANDOM!"
sentAt=$(now_iso)

# Create Recognition
create_recog_resp=$(curl_command "POST" "$BASE_URL/recognitions" "{\n  \"recognitionTypeId\": 1,\n  \"recognitionTypeUuid\": \"$recognitionTypeUuid\",\n  \"recipientId\": 2,\n  \"recipientUuid\": \"$recipientUuid\",\n  \"senderId\": 3,\n  \"senderUuid\": \"$senderUuid\",\n  \"awardName\": \"$awardName\",\n  \"level\": \"$level\",\n  \"message\": \"$message\",\n  \"awardPoints\": 100,\n  \"sentAt\": \"$sentAt\"\n}" "application/json" "Create Recognition")

# Extract recognition ID/UUID for chaining
recog_id=$(echo "$create_recog_resp" | grep -o '"id"[ ]*:[ ]*[0-9]*' | head -1 | awk -F: '{print $2}' | tr -d ' ')
recog_uuid=$(echo "$create_recog_resp" | grep -o '"uuid"[ ]*:[ ]*"[^"]*"' | head -1 | awk -F: '{print $2}' | tr -d '" ')

# List Recognitions
curl_command "GET" "$BASE_URL/recognitions?page=0&size=20&id=$recog_id&uuid=$recog_uuid&name=$awardName" "" "" "List Recognitions"

# Get Recognition
curl_command "GET" "$BASE_URL/recognitions/single?id=$recog_id" "" "" "Get Recognition by ID"
curl_command "GET" "$BASE_URL/recognitions/single?uuid=$recog_uuid" "" "" "Get Recognition by UUID"

# Update Recognition
curl_command "PUT" "$BASE_URL/recognitions/single?id=$recog_id&uuid=$recog_uuid" "{\n  \"awardName\": \"$awardName\",\n  \"level\": \"$level\",\n  \"message\": \"Outstanding $RANDOM!\",\n  \"awardPoints\": 120,\n  \"approvalStatus\": \"APPROVED\",\n  \"rejectionReason\": \"\"\n}" "application/json" "Update Recognition"

# Delete Recognition
curl_command "DELETE" "$BASE_URL/recognitions/single?id=$recog_id&uuid=$recog_uuid" "" "" "Delete Recognition"

# 2. Approval Actions

# Approve Recognition
curl_command "PATCH" "$BASE_URL/recognitions/approve?id=$recog_id&uuid=$recog_uuid&approverId=5" '{"reason": "Excellent performance"}' "application/json" "Approve Recognition"

# Reject Recognition
curl_command "PATCH" "$BASE_URL/recognitions/reject?id=$recog_id&uuid=$recog_uuid&approverId=5&reason=Not+eligible" '{"reason": "Not eligible for this period"}' "application/json" "Reject Recognition"

# 3. Search & Filtering

# Search Recognitions
curl_command "GET" "$BASE_URL/recognitions/search?name=Employee%20of%20the%20Month&unitId=1&typeId=3&points=100&id=$recog_id&uuid=$recog_uuid&role=Manager&managerId=2&awardName=Employee%20of%20the%20Month&status=APPROVED&page=0&size=20" "" "" "Search Recognitions"

# 4. Export

# Export Recognitions as CSV
curl_command "GET" "$BASE_URL/recognitions/export.csv?recipientId=2&senderId=3&role=Manager&managerId=5&days=30&id=$recog_id&uuid=$recog_uuid&unitId=1&typeId=3&status=APPROVED&points=100&awardName=Employee%20of%20the%20Month" "" "text/csv" "Export Recognitions CSV"

# Export Recognitions as JSON
curl_command "GET" "$BASE_URL/recognitions/export.json?recipientId=2&senderId=3&role=Manager&managerId=5&days=30&id=$recog_id&uuid=$recog_uuid&unitId=1&typeId=3&status=APPROVED&points=100&awardName=Employee%20of%20the%20Month" "" "application/json" "Export Recognitions JSON"

# Export Recognitions as TOON
curl_command "GET" "$BASE_URL/recognitions/export.toon?recipientId=2&senderId=3&role=Manager&managerId=5&days=30&id=$recog_id&uuid=$recog_uuid&unitId=1&typeId=3&status=APPROVED&points=100&awardName=Employee%20of%20the%20Month" "" "application/octet-stream" "Export Recognitions TOON"

# 5. Analytics

# Top Senders
curl_command "GET" "$BASE_URL/recognitions/top-senders?days=90&unitId=1&role=Manager&managerId=2" "" "" "Top Senders"

# Top Recipients
curl_command "GET" "$BASE_URL/recognitions/top-recipients?days=90&unitId=1&role=Manager&managerId=2" "" "" "Top Recipients"

# Insights
curl_command "GET" "$BASE_URL/recognitions/insights?days=365&unitId=1&role=Manager&managerId=2" "" "" "Insights"

# 6. Graphs

# Generate Recognition Graph
curl_command "GET" "$BASE_URL/recognitions/graph?id=$recog_id&uuid=$recog_uuid&name=Employee%20of%20the%20Month&unitId=1&role=Manager&senderId=3&senderUuid=123e4567-e89b-12d3-a456-426614174002&receiverId=2&receiverUuid=123e4567-e89b-12d3-a456-426614174001&managerId=5&awardName=Employee%20of%20the%20Month&points=100&status=APPROVED&typeId=3&days=30&weeks=10&months=6&years=2&groupBy=week&iterations=10" "" "image/png" "Generate Recognition Graph"

# 7. Employee Management

firstName="John"
lastName="Doe"
unitId=1
managerId=2
email=$(rand_email)
joiningDate=$(now_iso)
role="employee"

# Create Employee
create_emp_resp=$(curl_command "POST" "$BASE_URL/employees" "{\n  \"firstName\": \"$firstName\",\n  \"lastName\": \"$lastName\",\n  \"unitId\": $unitId,\n  \"managerId\": $managerId,\n  \"email\": \"$email\",\n  \"joiningDate\": \"$joiningDate\",\n  \"role\": \"$role\"\n}" "application/json" "Create Employee")

# Extract employee ID/UUID for chaining
emp_id=$(echo "$create_emp_resp" | grep -o '"id"[ ]*:[ ]*[0-9]*' | head -1 | awk -F: '{print $2}' | tr -d ' ')
emp_uuid=$(echo "$create_emp_resp" | grep -o '"uuid"[ ]*:[ ]*"[^"]*"' | head -1 | awk -F: '{print $2}' | tr -d '" ')

# List Employees
curl_command "GET" "$BASE_URL/employees?page=0&size=20&id=$emp_id&uuid=$emp_uuid&name=$firstName%20$lastName&role=Manager&unitId=1&managerId=2" "" "" "List Employees"

# Get Employee
curl_command "GET" "$BASE_URL/employees/single?id=$emp_id" "" "" "Get Employee by ID"
curl_command "GET" "$BASE_URL/employees/single?uuid=$emp_uuid" "" "" "Get Employee by UUID"

# Update Employee
curl_command "PUT" "$BASE_URL/employees/single?id=$emp_id&uuid=$emp_uuid" "{\n  \"firstName\": \"Jane\",\n  \"lastName\": \"Smith\",\n  \"unitId\": 2,\n  \"managerId\": 3,\n  \"email\": \"jane.smith@example.com\",\n  \"joiningDate\": \"2025-02-01\",\n  \"role\": \"manager\"\n}" "application/json" "Update Employee"

# Delete Employee
curl_command "DELETE" "$BASE_URL/employees/single?id=$emp_id&uuid=$emp_uuid" "" "" "Delete Employee"

# Search Employees
curl_command "GET" "$BASE_URL/employees/search?name=John%20Doe&unitId=1&id=$emp_id&uuid=$emp_uuid&role=Manager&managerId=2&page=0&size=20" "" "" "Search Employees"

# 8. Recognition Type Management

typeName="Appreciation"

# Create Recognition Type
create_recog_type_resp=$(curl_command "POST" "$BASE_URL/recognition-types" "{\n  \"typeName\": \"$typeName\"\n}" "application/json" "Create Recognition Type")

# Extract recognition type ID/UUID for chaining
recog_type_id=$(echo "$create_recog_type_resp" | grep -o '"id"[ ]*:[ ]*[0-9]*' | head -1 | awk -F: '{print $2}' | tr -d ' ')
recog_type_uuid=$(echo "$create_recog_type_resp" | grep -o '"uuid"[ ]*:[ ]*"[^"]*"' | head -1 | awk -F: '{print $2}' | tr -d '" ')

# List Recognition Types
curl_command "GET" "$BASE_URL/recognition-types?page=0&size=20&name=$typeName&id=$recog_type_id&uuid=$recog_type_uuid" "" "" "List Recognition Types"

# Get Recognition Type
curl_command "GET" "$BASE_URL/recognition-types/single?id=$recog_type_id" "" "" "Get Recognition Type by ID"
curl_command "GET" "$BASE_URL/recognition-types/single?uuid=$recog_type_uuid" "" "" "Get Recognition Type by UUID"

# Update Recognition Type
curl_command "PUT" "$BASE_URL/recognition-types/single?id=$recog_type_id&uuid=$recog_type_uuid" "{\n  \"typeName\": \"Kudos\"\n}" "application/json" "Update Recognition Type"

# Delete Recognition Type
curl_command "DELETE" "$BASE_URL/recognition-types/single?id=$recog_type_id&uuid=$recog_type_uuid" "" "" "Delete Recognition Type"

# Search Recognition Types
curl_command "GET" "$BASE_URL/recognition-types/search?name=Appreciation&id=$recog_type_id&uuid=$recog_type_uuid&page=0&size=20" "" "" "Search Recognition Types"

# 9. Metrics

# Metrics: Summary
curl_command "GET" "$BASE_URL/metrics/summary?days=30" "" "" "Metrics Summary"

# Metrics: Status
curl_command "GET" "$BASE_URL/metrics/status" "" "" "Metrics Status"

# Print summary of all results
echo "\n=================="
echo "Endpoint Test Summary:"
for result in "${results[@]}"; do
  echo "$result"
done
