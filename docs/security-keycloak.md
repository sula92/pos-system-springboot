# Security with Keycloak

The POS API is a stateless OAuth2 resource server. Every request must carry a Keycloak access token (`Authorization: Bearer <token>`). Requests without a valid token get `401`; requests with a valid token but the wrong role get `403`.

## Roles and access

Roles are hierarchical: `super_admin` includes `admin`, and `admin` includes `user`.

| Endpoint | user | admin | super_admin |
|---|---|---|---|
| `GET /customer`, `/customer/search`, `/customer/by-email` | yes | yes | yes |
| `GET /item`, `/item/search`, `/item/by-min-price` | yes | yes | yes |
| `GET /inventory`, `/inventory/low-stock`, `/inventory/in-stock` | yes | yes | yes |
| `GET /order` | yes | yes | yes |
| `POST /order` (place order) | yes | yes | yes |
| `POST /customer`, `PUT /customer` | yes | yes | yes |
| `POST /item`, `PUT /item` | no | yes | yes |
| `POST /inventory`, `PUT /inventory` | no | yes | yes |
| Reports: `/customer/purchase-stats*`, `/inventory/stock-view`, `/inventory/stock-value-view`, `/order/summary*` | no | yes | yes |
| `DELETE /customer`, `DELETE /item` | no | no | yes |

Any endpoint not in this table is denied. The rules live in `SecurityConfig`.

Roles are read from the token's realm roles (`realm_access.roles`) and from the client roles of `KEYCLOAK_CLIENT_ID` (`resource_access.<client>.roles`).

## Configuration

| Variable | Default | Purpose |
|---|---|---|
| `KEYCLOAK_ISSUER_URI` | `http://localhost:8180/realms/pos` | Realm issuer. Must equal the token's `iss` claim. |
| `KEYCLOAK_CLIENT_ID` | `pos-frontend` | Client whose client roles are also mapped. |
| `POS_CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Comma-separated browser origins allowed to call the API. |

## Local setup

1. Start Keycloak with the `pos` realm (roles and the `pos-frontend` client are imported):
   ```
   KEYCLOAK_ADMIN_PASSWORD=<choose-one> docker compose -f docker-compose.keycloak.yml up -d
   ```
2. In the admin console (http://localhost:8180), open realm `pos`, create users, set their passwords, and assign realm roles `user`, `admin` or `super_admin`.
3. Get a token for testing (the dev client allows the password grant):
   ```
   curl -s -d client_id=pos-frontend -d grant_type=password \
        -d username=<user> -d password=<password> \
        http://localhost:8180/realms/pos/protocol/openid-connect/token
   ```
4. Call the API: `curl -H "Authorization: Bearer <access_token>" http://localhost:8080/pos-system/item`

For production, disable the password grant on the client and use the authorization code flow with PKCE from the frontend.
