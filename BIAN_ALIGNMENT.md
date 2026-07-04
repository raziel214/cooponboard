# BIAN Alignment - Customer Onboarding (coop-customer-onboarding)

This microservice aligns with the **Customer Onboarding** service domain as defined by the Banking Industry Architecture Network (BIAN).

## Service Domain Profile
- **Service Domain Name:** Customer Onboarding
- **BIAN Functional Area:** Customer Relationship Management
- **Description:** Coordinates the intake, evaluation, document storage, and activation of new cooperative members.

## Semantic API Mapping

| BIAN Service Operation | HTTP Method | REST Endpoint | Description |
|---|---|---|---|
| **Initiate** | `POST` | `/api/v1/onboarding` | Initializes the onboarding request with member details. |
| **Request Document Upload** | `POST` | `/api/v1/onboarding/{id}/documents` | Uploads supporting documents (ID card, proof of income) to MinIO. |
| **Retrieve Status** | `GET` | `/api/v1/onboarding/{id}` | Fetches the current lifecycle state of the onboarding request. |
| **Evaluate & Complete** | `POST` | `/api/v1/onboarding/{id}/evaluate` | Approves or rejects the membership request. |

## Lifecycle States
- **INITIATED**: Request received. Member data validated syntactically.
- **DOCUMENTS_UPLOADED**: Required documents stored in S3/MinIO.
- **ELIGIBILITY_CHECKED**: Automated checks completed.
- **APPROVED**: Membership active.
- **REJECTED**: Onboarding failed.
