| # | group # | entity | fn | path | curl |
| --- | --- | --- | --- | --- | --- |
| 1 | A.1 | completion | Internal only; list final deliveries with attempts; timestamp/ID cursor; size 1–20; Bearer authentication | `GET /internal/v1/delivery-completions?after={timestamp}&afterId={deliveryId}&size={size}` | `curl -i -X GET 'http://127.0.0.1:8082/internal/v1/delivery-completions?after=1970-01-01T00%3A00%3A00Z&afterId=00000000-0000-0000-0000-000000000000&size=20' -H 'Authorization: Bearer example_internal_credential_replace_with_configured_value'` |

The curl uses an example credential; replace it with the configured shared internal token. No user, cookie or UI APIs in B.
