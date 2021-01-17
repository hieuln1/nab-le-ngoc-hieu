# nab-le-ngoc-hieu

## Description
### Solution
The project have 2 serparated services:
1. voucher-service: main service for
  * Get voucher code: Assume the payment has been done and front end have the payment id (`purchaseId`), then `frontend` call `voucher-service` to get voucher code. Then `voucher-service` will call `third-party-service` to get voucher code. If third party return voucher within 30 sec, `voucher-service` will reuturn voucher code as response. Else `voucher-service` will return message to user, voucher code will be sent to user via SMS by mock `SmsService`.
  * List voucher code: User have input `OTP` in order to get list of voucher code by phone number
2. third-party-service: mock third party service that return the voucher code with random latency from 3 to 120 seconds.

### Technical
* The project use `Spring Boot with Java 8`.
* Database use `Postgres`
* Build with `gradle`
* Deployed with `Docker`
* Test with `JUnit`, `Mockito`, embeeded database `zonky`

  In order to return timeout message as response, we use `Async` call to third party to get voucher via REST. If third party cannot handle in 30s, we set state of voucher as `TIMEOUT` then return timeout message as response. Then when got voucher from third party, we query state of the voucher, if it's `TIMEMOUT` we call `SmsService` to send voucher to user via SMS.
  
  To ensure thread safe, we use a `lock` object to `syncronized` between the waiting in `controller` and the processing in `service`
  
## Sequence Diagram
### Get voucher sequence diagram
![Get voucher sequence diagram](./get-voucher-sequence-diagram.png)
### List voucher sequence diagram
![List voucher sequence diagram](./list-voucher-sequence-diagram.png)
  
## Database
The project just have 2 simple table with no relationship

```
                           Table "public.voucher"
    Column    |            Type             | Collation | Nullable | Default
--------------+-----------------------------+-----------+----------+---------
 id           | uuid                        |           | not null |
 created_at   | timestamp without time zone |           |          |
 phone_number | character varying(255)      |           |          |
 purchase_id  | character varying(255)      |           |          |
 state        | character varying(255)      |           |          |
 updated_at   | timestamp without time zone |           |          |
 voucher_code | character varying(255)      |           |          |

```
```
                           Table "public.voucher"
    Column    |            Type             | Collation | Nullable | Default
--------------+-----------------------------+-----------+----------+---------
 id           | uuid                        |           | not null |
 created_at   | timestamp without time zone |           |          |
 phone_number | character varying(255)      |           |          |
 purchase_id  | character varying(255)      |           |          |
 state        | character varying(255)      |           |          |
 updated_at   | timestamp without time zone |           |          |
 voucher_code | character varying(255)      |           |          |
 ```
## How to run
### Start project
  The project can run on docker, we already have docker-compose for both service and database put in voucher-service. 
  
  Move to voucher-service folder then run restart.sh script
```
cd ./voucher-service

./restart.sh
```
If don't have permission we need to grand permission
```
chmod +x ./restart.sh
```

### Get voucher
Call request to get voucher with provied phone number and purchase id
```
curl --location --request POST 'http://localhost:8080/voucher' \
--header 'Content-Type: application/json' \
--data-raw '{
    "phoneNumber":"0907769634",
    "purchaseId":"12348"
}'
```
Check the log for simulation time of third party, if simulate process duration is greater than 30s, voucher will be sent via SMS
```
{
  "success": true,
  "message": "The request is being processed, voucher will be sent via SMS later",
  "voucherCode": null
}
```

```third-party-service | 2021-01-17 00:55:14.420  INFO 1 --- [nio-8081-exec-1] c.e.nab.controller.ThirdPartyController  : Received get voucher request to phone number 0907769634 with purchase id 12349
third-party-service | 2021-01-17 00:55:14.423  INFO 1 --- [nio-8081-exec-1] c.e.nab.controller.ThirdPartyController  : Simulate processing in 90 seconds
voucher-service | 2021-01-17 00:55:43.769  INFO 1 --- [nio-8080-exec-1] c.e.v.controller.VoucherController       : Cannot get voucher code within 30s, voucher will be sent via SMS later
third-party-service | 2021-01-17 00:56:44.406  INFO 1 --- [nio-8081-exec-1] c.e.nab.controller.ThirdPartyController  : Process generate voucher finished
voucher-service | 2021-01-17 00:56:44.554  INFO 1 --- [         task-1] c.e.v.service.VoucherService             : Sending voucher to client via SMS
voucher-service | 2021-01-17 00:56:44.555  INFO 1 --- [         task-1] c.e.voucherservice.service.SmsService    : Mock sending SMS to phone number 0907769634 with message Your voucher of purchase 12349 is: 80b06b02-864d-4267-8ab7-d6dd0e0ee467
```
Else if simulate process duration less than 30s, voucher will be return as response
```
{
  "success": true,
  "message": null,
  "voucherCode": "de43fedf-1d2e-428f-b183-9266acb49f5f"
}
```
```
third-party-service | 2021-01-17 01:01:15.570  INFO 1 --- [nio-8081-exec-3] c.e.nab.controller.ThirdPartyController  : Received get voucher request to phone number 0907769634 with purchase id 12350
third-party-service | 2021-01-17 01:01:15.570  INFO 1 --- [nio-8081-exec-3] c.e.nab.controller.ThirdPartyController  : Simulate processing in 20 seconds
third-party-service | 2021-01-17 01:01:35.571  INFO 1 --- [nio-8081-exec-3] c.e.nab.controller.ThirdPartyController  : Process generate voucher finished
voucher-service | 2021-01-17 01:01:35.597  INFO 1 --- [         task-2] c.e.v.service.VoucherService             : Sending voucher to client via REST response
```

### Request OTP
```
curl --location --request POST 'http://localhost:8080/otp/request?phone_number=0907769634'
{
    "success": true,
    "message": null,
    "otpRequestId": "6a692edb-d62e-4e40-a2fc-4d8a0ef982fd"
}
```
Check the log for OTP value
```
voucher-service | 2021-01-17 01:08:32.167  INFO 1 --- [nio-8080-exec-9] c.e.v.controller.OtpController           : Requesting OTP for phone number 0907769634
voucher-service | 2021-01-17 01:08:32.168  INFO 1 --- [nio-8080-exec-9] c.e.voucherservice.service.SmsService    : Mock sending SMS to phone number 0907769634 with message Your OTP is: 560949. It will expire in 3 minutes
```

### Verify OTP
Verify OTP with OTP value from log and `requestId` from response of request OTP
```
curl --location --request POST 'http://localhost:8080/otp/verify' \
--header 'Content-Type: application/json' \
--data-raw '{
    "requestId":"6a692edb-d62e-4e40-a2fc-4d8a0ef982fd",
    "otp":"560949"
}'

{
    "success": true,
    "message": "OK"
}
```
### Get list voucher by phone number
Get list vouche by phone number with verified request OTP id above
```
curl --location --request GET 'http://localhost:8080/voucher?verification_id=6a692edb-d62e-4e40-a2fc-4d8a0ef982fd&phone_number=0907769634'

[
    "80b06b02-864d-4267-8ab7-d6dd0e0ee467",
    "de43fedf-1d2e-428f-b183-9266acb49f5f"
]
```

## Not done in project
* We can do the OTP service and voucher service as separated service, using it's own DB and communicate via REST
