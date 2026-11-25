# YOLO Chat Frontend

A React chat widget that talks directly to `ussd-gateway-service`, giving the
same Yolo menu (Gwamon', YOLO Voice, YOLO Internet, etc.) a button-driven chat
UI instead of dialing `*154#`, plus a free-text box for FAQ-style questions
about how the system works.

It does **not** replace or connect to real USSD phone sessions — it's a
separate, parallel channel that reuses the same backend logic
(`UssdSessionService`, bundle/payment clients) inside `ussd-gateway-service`.

## Backend endpoints

The gateway provides two JSON endpoints for the frontend:

- `POST /api/menu/navigate` — `{ sessionId, phoneNumber?, input }` returns
  `{ sessionId, message, buttons: [{label, value}], isEnd, inputType }`.
  Reuses the same session and menu logic as the `/ussd` endpoint.
- `POST /chat/ask` — `{ sessionId, input }` returns `{ sessionId, message }`.
  Simple keyword-based FAQ matching (`FaqService.kt`), no external AI call,
  no session interaction — asking a question never disturbs the user's place
  in the menu.

## Running the frontend locally

Start the three backend services as described in the repository's top-level
README, then run:

```bash
cd chat-frontend
npm install
npm run dev
```

Open the printed local URL (usually `http://localhost:5173`).

By default it calls `ussd-gateway-service` at `http://localhost:8080`. Change
`API_BASE` at the top of `src/ChatWidget.jsx` if your deployment runs
elsewhere.

## Notes

- Each browser tab gets its own session id (stored in `sessionStorage`), so
  refreshing keeps your place in the menu; a new tab starts fresh.
- Screens with no numbered options return `inputType: "FREE_TEXT"` and accept
  typed menu responses through the chat input.
- CORS is currently wide open (`@CrossOrigin(origins = ["*"])`) on the two
  new endpoints only, to make local dev easy. Tighten this to your actual
  frontend's origin before going to production.
