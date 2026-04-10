sequenceDiagram
  autonumber
  participant SPA as Angular SPA
  participant SP as Spring Boot Resource Server
  participant AS as PingOne AS Authorization Server
  participant PDP as PingOne Authorize PDP decision

  Note over SPA,AS: PHASE 1 - Authentification initiale scope simple sans RAR

  SPA->>AS: GET /authorize client_id redirect_uri scope=openid profile code_challenge PKCE
  Note right of AS: client_id valide et actif
  Note right of AS: redirect_uri enregistree exacte
  Note right of AS: scopes autorises pour ce client
  Note right of AS: code_challenge present PKCE obligatoire

  AS-->>SPA: Redirect vers page de login PingOne

  SPA->>AS: POST credentials login et mot de passe utilisateur
  Note right of AS: Credentials valides via PingOne Directory
  Note right of AS: Compte actif accountActive egal true
  Note right of AS: Compte non bloque MFA si requis

  AS-->>SPA: Redirect vers redirect_uri avec code autorisation

  SPA->>AS: POST /token grant_type=authorization_code code code_verifier PKCE
  Note right of AS: code non expire et non deja utilise
  Note right of AS: code_verifier correspond au code_challenge
  Note right of AS: client correctement authentifie

  AS-->>SPA: access_token JWT plus id_token claims sub groups transferLimit ibanAccount

  Note over SPA,PDP: PHASE 2 - Clic sur Voir mes paiements lecture simple pas de RAR

  SPA->>SP: GET /api/payments Authorization Bearer access_token
  Note right of SP: Signature JWT valide via cle publique AS
  Note right of SP: Audience aud correspond au Resource Server
  Note right of SP: Token non expire exp
  Note right of SP: Issuer iss correct
  Note right of SP: Scope suffisant pour la lecture
  Note right of SP: Extraction claim groups vers ROLE_PAYERS

  SP->>PDP: POST /decisions actionId VIEW_OWN_PAYMENTS userId sub groups PAYERS creatorId sub
  Note right of PDP: groups contient PAYERS ou MANAGERS
  Note right of PDP: userId egal creatorId pour ses propres paiements
  Note right of PDP: Si MANAGERS peut tout voir sans restriction

  PDP-->>SP: decision PERMIT

  SP-->>SPA: 200 OK liste des paiements

  Note over SPA,PDP: PHASE 3 - Initiation virement PAR plus RAR action sensible

  SPA->>AS: POST /as/par PAR RFC 9126 authorization_details type wire_transfer amount devise debtorIBAN creditorIBAN purpose scope=openid profile wire_transfer code_challenge PKCE
  Note right of AS: Client authentifie correctement
  Note right of AS: Type wire_transfer enregistre cote AS
  Note right of AS: Schema JSON du RAR valide champs requis presents
  Note right of AS: redirect_uri enregistree exacte
  Note right of AS: PKCE code_challenge present
  Note right of AS: Requete stockee cote AS identifiant court genere

  AS-->>SPA: request_uri valable 90 secondes

  SPA->>AS: GET /authorize client_id request_uri
  Note right of AS: request_uri valide et non expire
  Note right of AS: Associe au bon client_id

  AS-->>SPA: Redirect vers consentement ou login si session expiree

  SPA->>AS: POST /token grant_type=authorization_code code code_verifier
  Note right of AS: PKCE verifie code_verifier vs code_challenge
  Note right of AS: Code valide et non rejoue
  Note right of AS: authorization_details injectes dans le JWT signe

  AS-->>SPA: access_token JWT enrichi avec authorization_details wire_transfer

  SPA->>SP: POST /api/payments Authorization Bearer access_token enrichi body amount debtorIBAN creditorIBAN
  Note right of SP: Signature JWT plus exp plus aud plus iss valides
  Note right of SP: authorization_details.type egal wire_transfer
  Note right of SP: authorization_details.amount egal body.amount
  Note right of SP: authorization_details.debtorIBAN egal body.debtorIBAN
  Note right of SP: authorization_details.debtorIBAN egal claim ibanAccount
  Note right of SP: Si ecart detecte rejet 403 RAR mismatch

  SP->>PDP: POST /decisions actionId CREATE_PAYMENT userId sub groups PAYERS amount transferLimit accountActive creatorId sub
  Note right of PDP: groups contient PAYERS
  Note right of PDP: accountActive egal true
  Note right of PDP: amount inferieur ou egal a transferLimit
  Note right of PDP: Si amount superieur a transferLimit DENY obligation approbation manager

  PDP-->>SP: decision PERMIT

  SP-->>SPA: 201 Created paiement enregistre
