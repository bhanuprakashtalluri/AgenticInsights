# Example cURL Commands for API Testing (Current State)

## Login
```
curl -c cookies.txt -X POST http://localhost:8080/api/auth/login -H 'Content-Type: application/json' -d '{"username":"alice.smith@company.com","password":"Manager@123"}'
```

## Get Current User Info
```
curl -b cookies.txt http://localhost:8080/api/auth/me
```

## Update Password
```
curl -b cookies.txt -X PUT http://localhost:8080/api/auth/update-password -H 'Content-Type: application/x-www-form-urlencoded' -d 'email=alice.smith@company.com&newPassword=NewPass@123'
```

## List Employees (as Manager)
```
curl -b cookies.txt http://localhost:8080/employees
```

## List Recognitions (as Employee)
```
curl -b cookies.txt http://localhost:8080/recognitions
```

## Delete Employee (as Manager)
```
curl -b cookies.txt -X DELETE http://localhost:8080/employees/single?id=2
```

## Get Insights
```
curl -b cookies.txt http://localhost:8080/insights
```

## Export Recognitions as CSV
```
curl -b cookies.txt http://localhost:8080/recognitions/export.csv
```

---

*Make sure to login first and use the session cookie for all protected endpoints.*

