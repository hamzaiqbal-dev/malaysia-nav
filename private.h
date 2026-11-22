#pragma once

#define OSM_CONSUMER_KEY "zz16eUusTr4TcVhoKQrgOJaRzZAeI2ktbVx6e3yn"
#define OSM_CONSUMER_SECRET "7g4XrlhFkiOG1I3Zpqh8ajo94P9cNNXaU2YCMgL7"
#define OSM_OAUTH2_CLIENT_ID "3iB16A21eY8HvxJpPbdih94Us1Po5NfqoWHaZaaCd50"
#define OSM_OAUTH2_CLIENT_SECRET "AKyf1iUliVQ6CWJWQV7NQcx2T1Icr013k5sb7JSK4eU"
#define OSM_OAUTH2_REDIRECT_URI "om://oauth2/osm/callback"
#define OSM_OAUTH2_SCOPE "read_prefs write_api write_notes"
#define MWM_GEOLOCATION_SERVER ""
// Sampai map files: <server>maps/<version>/<file>.mwm, published by .github/workflows/sampai-maps.yaml.
// No metaserver: the app uses DEFAULT_URLS_JSON directly. See docs/my/MAPS.md.
#define METASERVER_URL ""
#define DIFF_LIST_URL ""
#define DEFAULT_URLS_JSON \
  R"({"servers":["https://github.com/hamzaiqbal-dev/malaysia-nav/releases/download/"]})"
#define DEFAULT_CONNECTION_CHECK_IP "1.1.1.1"  // Linux desktop only: TCP port 80 reachability check
#define TRAFFIC_DATA_BASE_URL ""
#define USER_BINDING_PKCS12 ""
#define USER_BINDING_PKCS12_PASSWORD ""
#define KAYAK_AFFILIATE_ID "kan_267335"
