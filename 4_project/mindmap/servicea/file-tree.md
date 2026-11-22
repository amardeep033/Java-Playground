

| 1 | apikey | `apikey/dto/ApiKeyDetails.java` | 🟢 |
| 2 | apikey | `apikey/dto/CreateApiKey.java` | 🟢 |
| 3 | apikey | `apikey/dto/IssuedApiKey.java` | 🟢 |
| 4 | apikey | `apikey/ApiKey.java` | 🟢 |
| 5 | apikey | `apikey/ApiKeyController.java` | 🟢 |
| 6 | apikey | `apikey/ApiKeyRepository.java` | 🔴 |
| 7 | apikey | `apikey/ApiKeyService.java` | 🔴 |


| 8 | app | `application/dto/ApplicationResponse.java` | 🟢 |
| 9 | app | `application/dto/ApplicationUpdateFields.java` | 🟢 |
| 10 | app | `application/dto/RegisterApplication.java` | 🟢 |
| 11 | app | `application/dto/UpdateApplication.java` | 🟢 |
| 12 | app | `application/Application.java` | 🟢 |
| 13 | app | `application/ApplicationController.java` | 🟢 |
| 14 | app | `application/ApplicationRepository.java` | 🔴 |
| 15 | app | `application/ApplicationService.java` | 🔴 |


| 16 | auth | `auth/access/ResourceAccess.java` | 🔴 |
| 17 | auth | `auth/access/WorkspaceAccess.java` | 🔴 |
| 18 | auth | `auth/AuthController.java` | 🔴 |
| 19 | auth | `auth/AuthService.java` | 🔴 |
| 20 | auth | `auth/InternalServiceToken.java` | 🔴 |
| 21 | auth | `auth/JwtAuthenticationFilter.java` | 🟢 |
| 22 | auth | `auth/JwtCookies.java` | 🟢 |
| 23 | auth | `auth/JwtTokens.java` | 🟢 |
| 24 | auth | `auth/JwtUser.java` | 🟢 |
| 25 | auth | `auth/RefreshTokens.java` | 🔴 |


| 26 | common | `common/config/CredentialConfig.java` | 🔴 |
| 27 | common | `common/config/OwnershipConfig.java` | 🔴 |
| 28 | common | `common/config/RabbitMqConfig.java` | 🟢 |
| 29 | common | `common/config/SecurityConfig.java` | 🟢 |
| 30 | common | `common/crypto/SecretEncryption.java` | 🔴 |
| 31 | common | `common/dto/PageResult.java` | 🟢 |
| 32 | common | `common/dto/UpdateName.java` | 🟢 |
| 33 | common | `common/exception/ApiExceptionHandler.java` | 🔴 |
| 34 | common | `common/persistence/BaseEntity.java` | 🟢 |
| 35 | common | `common/search/ListSearch.java` | 🔴 |
| 36 | common | `common/security/ApplicationAuthorization.java` | 🟢 |


| 37 | delivery | `delivery/client/DeliveryCompletionClient.java` | 🟢 |
| 38 | delivery | `delivery/messaging/DeliveryCommand.java` | 🟢 |
| 39 | delivery | `delivery/messaging/DeliveryStatusMessage.java` | 🟢 |
| 40 | delivery | `delivery/messaging/StatusListener.java` | 🟢 |
| 41 | delivery | `delivery/messaging/StatusUpdateService.java` | 🟢 |
| 42 | delivery | `delivery/outbox/Outbox.java` | 🟢 |
| 43 | delivery | `delivery/persistence/DeliveryProjectionStore.java` | 🟢 |
| 44 | delivery | `delivery/reconciliation/ReconciliationService.java` | 🟢 |
| 45 | delivery | `delivery/reconciliation/ReconciliationWorker.java` | 🟢 |


| 46 | endpoint | `endpoint/dto/AdminEndpointUpdate.java` | 🟢 |
| 47 | endpoint | `endpoint/dto/EndpointConfiguration.java` | 🟢 |
| 48 | endpoint | `endpoint/dto/EndpointDeliveryConfiguration.java` | 🔴 |
| 49 | endpoint | `endpoint/dto/EndpointSettings.java` | 🔴 |
| 50 | endpoint | `endpoint/dto/IssuedEndpoint.java` | 🟢 |
| 51 | endpoint | `endpoint/dto/RegisterEndpoint.java` | 🟢 |
| 52 | endpoint | `endpoint/dto/UpdateEndpoint.java` | 🟢 |
| 53 | endpoint | `endpoint/dto/WebhookEndpoint.java` | 🟢 |
| 54 | endpoint | `endpoint/Endpoint.java` | 🟢 |
| 55 | endpoint | `endpoint/EndpointController.java` | 🟢 |
| 56 | endpoint | `endpoint/EndpointDeliveryConfig.java` | 🔴 |
| 57 | endpoint | `endpoint/EndpointDeliveryReader.java` | 🔴 |
| 58 | endpoint | `endpoint/EndpointRepository.java` | 🔴 |
| 59 | endpoint | `endpoint/EndpointService.java` | 🔴 |


| 60 | event | `event/dto/IngestRequest.java` | 🟢 |
| 61 | event | `event/dto/EventAccepted.java` | 🟢 |
| 62 | event | `event/dto/EventDeliveryView.java` | 🟢 |
| 63 | event | `event/dto/EventDetails.java` | 🟢 |
| 64 | event | `event/EventController.java` | 🟢 |
| 65 | event | `event/EventIngestionService.java` | 🟢 |
| 66 | event | `event/EventStore.java` | 🟢 |


| 67 | eventtype | `eventtype/dto/Activation.java` | 🟢 |
| 68 | eventtype | `eventtype/dto/EventTypeResponse.java` | 🟢 |
| 69 | eventtype | `eventtype/dto/RegisterEventType.java` | 🟢 |
| 70 | eventtype | `eventtype/dto/UpdateDescription.java` | 🟢 |
| 71 | eventtype | `eventtype/EventType.java` | 🟢 |
| 72 | eventtype | `eventtype/EventTypeController.java` | 🟢 |
| 73 | eventtype | `eventtype/EventTypeRepository.java` | 🔴 |
| 74 | eventtype | `eventtype/EventTypeService.java` | 🔴 |


| 75 | ingress | `ingress/dto/IncomingRequestEntry.java` | 🟢 |
| 76 | ingress | `ingress/IncomingRequestContext.java` | 🟢 |
| 77 | ingress | `ingress/IncomingRequestController.java` | 🟢 |
| 78 | ingress | `ingress/IncomingRequestFilter.java` | 🟢 |
| 79 | ingress | `ingress/IncomingRequestService.java` | 🟢 |
| 80 | ingress | `ingress/IncomingRequestStore.java` | 🟢 |


| 81 | monitoring | `monitoring/dto/EventSummary.java` | 🟢 |
| 82 | monitoring | `monitoring/dto/DeliverySummary.java` | 🟢 |
| 83 | monitoring | `monitoring/dto/DeliveryAttempt.java` | 🟢 |
| 84 | monitoring | `monitoring/dto/DeliveryDetails.java` | 🟢 |
| 85 | monitoring | `monitoring/dto/ReplayReceipt.java` | 🟢 |
| 86 | monitoring | `monitoring/dto/MonitoringSummary.java` | 🟢 |
| 87 | monitoring | `monitoring/MonitoringController.java` | 🟢 |
| 88 | monitoring | `monitoring/MonitoringService.java` | 🟢 |
| 89 | monitoring | `monitoring/MonitoringStore.java` | 🟢 |


| 90 | user | `user/dto/ChangePassword.java` | 🟢 |
| 91 | user | `user/dto/Signup.java` | 🟢 |
| 92 | user | `user/dto/UserResponse.java` | 🟢 |
| 93 | user | `user/UserAccount.java` | 🟢 |
| 94 | user | `user/UserAccountRepository.java` | 🔴 |
| 95 | user | `user/UserController.java` | 🟢 |
| 96 | user | `user/UserService.java` | 🔴 |


| 97 | workspace | `workspace/dto/AccountUpdate.java` | 🔴 |
| 98 | workspace | `workspace/dto/ClientUser.java` | 🔴 |
| 99 | workspace | `workspace/dto/ResetPassword.java` | 🔴 |
| 100 | workspace | `workspace/AuditLogs.java` | 🔴 |
| 101 | workspace | `workspace/ClientController.java` | 🔴 |
| 102 | workspace | `workspace/ClientService.java` | 🔴 |
| 103 | workspace | `workspace/WorkspaceController.java` | 🔴 |
| 104 | workspace | `workspace/WorkspaceService.java` | 🔴 |


| 105 | other | `ServiceAApplication.java` | 🟢 |
| 106 | other | `src/main/resources/application.properties` | 🟢 |
| 107 | other | `src/main/resources/db/schema.sql` | 🟢 |
| 108 | other | `pom.xml` | 🟢 |


| 109 | tests | `test` | 🔴 |

