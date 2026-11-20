

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
| 21 | auth | `auth/JwtAuthenticationFilter.java` | 🔴 |
| 22 | auth | `auth/JwtCookies.java` | 🔴 |
| 23 | auth | `auth/JwtTokens.java` | 🔴 |
| 24 | auth | `auth/JwtUser.java` | 🔴 |
| 25 | auth | `auth/RefreshTokens.java` | 🔴 |


| 26 | common | `common/config/CredentialConfig.java` | 🔴 |
| 27 | common | `common/config/OwnershipConfig.java` | 🔴 |
| 28 | common | `common/config/RabbitMqConfig.java` | 🟢 |
| 29 | common | `common/config/SecurityConfig.java` | 🔴 |
| 30 | common | `common/crypto/SecretEncryption.java` | 🔴 |
| 31 | common | `common/dto/PageResult.java` | 🟢 |
| 32 | common | `common/dto/UpdateName.java` | 🟢 |
| 33 | common | `common/exception/ApiExceptionHandler.java` | 🔴 |
| 34 | common | `common/persistence/BaseEntity.java` | 🟢 |
| 35 | common | `common/search/ListSearch.java` | 🔴 |


| 36 | delivery | `delivery/client/DeliveryCompletionClient.java` | 🔴 |
| 37 | delivery | `delivery/messaging/DeliveryCommand.java` | 🔴 |
| 38 | delivery | `delivery/messaging/DeliveryStatusMessage.java` | 🔴 |
| 39 | delivery | `delivery/messaging/StatusListener.java` | 🔴 |
| 40 | delivery | `delivery/messaging/StatusUpdateService.java` | 🔴 |
| 41 | delivery | `delivery/outbox/Outbox.java` | 🔴 |
| 42 | delivery | `delivery/persistence/DeliveryProjectionStore.java` | 🔴 |
| 43 | delivery | `delivery/reconciliation/ReconciliationService.java` | 🔴 |
| 44 | delivery | `delivery/reconciliation/ReconciliationWorker.java` | 🔴 |


| 45 | endpoint | `endpoint/dto/AdminEndpointUpdate.java` | 🟢 |
| 46 | endpoint | `endpoint/dto/EndpointConfiguration.java` | 🟢 |
| 47 | endpoint | `endpoint/dto/EndpointDeliveryConfiguration.java` | 🔴 |
| 48 | endpoint | `endpoint/dto/EndpointSettings.java` | 🔴 |
| 49 | endpoint | `endpoint/dto/IssuedEndpoint.java` | 🟢 |
| 50 | endpoint | `endpoint/dto/RegisterEndpoint.java` | 🟢 |
| 51 | endpoint | `endpoint/dto/UpdateEndpoint.java` | 🟢 |
| 52 | endpoint | `endpoint/dto/WebhookEndpoint.java` | 🟢 |
| 53 | endpoint | `endpoint/Endpoint.java` | 🟢 |
| 54 | endpoint | `endpoint/EndpointController.java` | 🟢 |
| 55 | endpoint | `endpoint/EndpointDeliveryConfig.java` | 🔴 |
| 56 | endpoint | `endpoint/EndpointDeliveryReader.java` | 🔴 |
| 57 | endpoint | `endpoint/EndpointRepository.java` | 🔴 |
| 58 | endpoint | `endpoint/EndpointService.java` | 🔴 |


| 59 | event | `event/dto/EventApi.java` | 🔴 |
| 60 | event | `event/EventController.java` | 🔴 |
| 61 | event | `event/EventIngestionService.java` | 🔴 |
| 62 | event | `event/EventStore.java` | 🔴 |


| 63 | eventtype | `eventtype/dto/Activation.java` | 🟢 |
| 64 | eventtype | `eventtype/dto/EventTypeResponse.java` | 🟢 |
| 65 | eventtype | `eventtype/dto/RegisterEventType.java` | 🟢 |
| 66 | eventtype | `eventtype/dto/UpdateDescription.java` | 🟢 |
| 67 | eventtype | `eventtype/EventType.java` | 🟢 |
| 68 | eventtype | `eventtype/EventTypeController.java` | 🟢 |
| 69 | eventtype | `eventtype/EventTypeRepository.java` | 🔴 |
| 70 | eventtype | `eventtype/EventTypeService.java` | 🔴 |


| 71 | ingress | `ingress/dto/IncomingRequestEntry.java` | 🔴 |
| 72 | ingress | `ingress/IncomingRequestContext.java` | 🔴 |
| 73 | ingress | `ingress/IncomingRequestController.java` | 🔴 |
| 74 | ingress | `ingress/IncomingRequestFilter.java` | 🔴 |
| 75 | ingress | `ingress/IncomingRequestService.java` | 🔴 |
| 76 | ingress | `ingress/IncomingRequestStore.java` | 🔴 |


| 77 | monitoring | `monitoring/dto/MonitoringApi.java` | 🔴 |
| 78 | monitoring | `monitoring/MonitoringController.java` | 🔴 |
| 79 | monitoring | `monitoring/MonitoringService.java` | 🔴 |
| 80 | monitoring | `monitoring/MonitoringStore.java` | 🔴 |


| 81 | user | `user/dto/ChangePassword.java` | 🟢 |
| 82 | user | `user/dto/Signup.java` | 🟢 |
| 83 | user | `user/dto/UserResponse.java` | 🟢 |
| 84 | user | `user/UserAccount.java` | 🟢 |
| 85 | user | `user/UserAccountRepository.java` | 🔴 |
| 86 | user | `user/UserController.java` | 🟢 |
| 87 | user | `user/UserService.java` | 🔴 |


| 88 | workspace | `workspace/dto/AccountUpdate.java` | 🔴 |
| 89 | workspace | `workspace/dto/ClientUser.java` | 🔴 |
| 90 | workspace | `workspace/dto/ResetPassword.java` | 🔴 |
| 91 | workspace | `workspace/AuditLogs.java` | 🔴 |
| 92 | workspace | `workspace/ClientController.java` | 🔴 |
| 93 | workspace | `workspace/ClientService.java` | 🔴 |
| 94 | workspace | `workspace/WorkspaceController.java` | 🔴 |
| 95 | workspace | `workspace/WorkspaceService.java` | 🔴 |


| 96 | other | `ServiceAApplication.java` | 🟢 |
| 97 | other | `src/main/resources/application.properties` | 🟢 |
| 98 | other | `src/main/resources/db/schema.sql` | 🟢 |
| 99 | other | `pom.xml` | 🟢 |


| 100 | tests | `test` | 🔴 |

