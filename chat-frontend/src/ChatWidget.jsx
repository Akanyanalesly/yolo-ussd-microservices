import { useState, useRef, useEffect } from "react";
import "./ChatWidget.css";

// Base URL of ussd-gateway-service (it now hosts /api/menu/navigate and
// /chat/ask alongside its existing /ussd endpoint). Point this at your
// deployed URL when you host it.
const API_BASE = "http://localhost:8080";

// Generates a stable session id for this browser tab.
function getSessionId() {
  let id = sessionStorage.getItem("yolo-chat-session-id");
  if (!id) {
    id = "web-" + Math.random().toString(36).slice(2) + "-" + Date.now();
    sessionStorage.setItem("yolo-chat-session-id", id);
  }
  return id;
}

// Keeps a browser tab identifiable across separate chat conversations without
// requiring a real customer phone number. It fits the gateway's 15-character
// MSISDN database field and looks like a local test number.
function getChatPhoneNumber() {
  let number = sessionStorage.getItem("yolo-chat-user-id");
  if (!/^078\d{7}$/.test(number || "")) {
    number = "078" + Math.floor(Math.random() * 10_000_000).toString().padStart(7, "0");
    sessionStorage.setItem("yolo-chat-user-id", number);
  }
  return number;
}

function createSessionId() {
  return "web-" + Math.random().toString(36).slice(2) + "-" + Date.now();
}

export default function ChatWidget() {
  const [sessionId, setSessionId] = useState(getSessionId);
  const [chatPhoneNumber] = useState(getChatPhoneNumber);
  const [messages, setMessages] = useState([]);
  const [freeText, setFreeText] = useState("");
  const [loading, setLoading] = useState(false);
  const [expectsMenuText, setExpectsMenuText] = useState(false);
  const [isMinimized, setIsMinimized] = useState(false);
  const [isMaximized, setIsMaximized] = useState(false);
  const [isClosed, setIsClosed] = useState(true);
  const bottomRef = useRef(null);
  const hasLoadedRootMenu = useRef(false);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  // Load the root menu when the chat is first opened.
  useEffect(() => {
    if (isClosed || hasLoadedRootMenu.current) return;
    hasLoadedRootMenu.current = true;
    sendMenuInput("START");
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isClosed]);

  async function sendMenuInput(input, targetSessionId = sessionId) {
    setLoading(true);
    try {
      const res = await fetch(`${API_BASE}/api/menu/navigate`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ sessionId: targetSessionId, phoneNumber: chatPhoneNumber, input }),
      });
      if (!res.ok) throw new Error(`Server returned ${res.status}`);
      const data = await res.json();
      setExpectsMenuText(data.inputType === "FREE_TEXT" && !data.isEnd);
      setMessages((prev) => [
        ...prev.filter((message) => !message.isMenu),
        {
          from: "bot",
          text: data.message,
          buttons: data.buttons || [],
          inputType: data.inputType || "BUTTONS",
          isEnd: data.isEnd,
          isMenu: true,
        },
      ]);
    } catch (err) {
      setMessages((prev) => [
        ...prev,
        { from: "bot", text: "Sorry, something went wrong reaching the server. Please try again." },
      ]);
      console.error(err);
    } finally {
      setLoading(false);
    }
  }

  async function sendAskQuestion(question) {
    setMessages((prev) => [...prev, { from: "user", text: question }]);
    setLoading(true);
    try {
      const res = await fetch(`${API_BASE}/chat/ask`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ sessionId, input: question }),
      });
      if (!res.ok) throw new Error(`Server returned ${res.status}`);
      const data = await res.json();
      setMessages((prev) => [...prev, { from: "bot", text: data.message, buttons: [] }]);
    } catch (err) {
      setMessages((prev) => [
        ...prev,
        { from: "bot", text: "Sorry, something went wrong reaching the server. Please try again." },
      ]);
      console.error(err);
    } finally {
      setLoading(false);
    }
  }

  function handleButtonClick(button) {
    if (loading) return;
    sendMenuInput(button.value);
  }

  function handleRestart() {
    const newSessionId = createSessionId();
    sessionStorage.setItem("yolo-chat-session-id", newSessionId);
    setSessionId(newSessionId);
    setMessages([]);
    setExpectsMenuText(false);
    sendMenuInput("START", newSessionId);
  }

  function handleAskSubmit(e) {
    e.preventDefault();
    const question = freeText.trim();
    if (!question) return;
    setFreeText("");
    if (expectsMenuText) {
      sendMenuInput(question);
    } else {
      sendAskQuestion(question);
    }
  }

  function toggleMinimize() {
    setIsMinimized((prev) => !prev);
    if (!isMinimized) setIsMaximized(false);
  }

  function toggleMaximize() {
    setIsMaximized((prev) => !prev);
    if (!isMaximized) setIsMinimized(false);
  }

  function handleClose() {
    setIsClosed(true);
    setIsMinimized(false);
    setIsMaximized(false);
  }

  function handleOpen() {
    setIsClosed(false);
    setIsMinimized(false);
    setIsMaximized(false);
  }

  if (isClosed) {
    return (
      <button
        type="button"
        className="chat-launcher"
        onClick={handleOpen}
        aria-label="Open YOLO Assistant chat"
        title="Open YOLO Assistant"
      >
        <svg viewBox="0 0 48 48" aria-hidden="true" focusable="false">
          <defs>
            <linearGradient id="robot-shell" x1="9" y1="10" x2="39" y2="37" gradientUnits="userSpaceOnUse">
              <stop stopColor="#59df7b" />
              <stop offset="1" stopColor="#ffcb05" />
            </linearGradient>
            <linearGradient id="robot-body" x1="15" y1="31" x2="34" y2="43" gradientUnits="userSpaceOnUse">
              <stop stopColor="#ffda42" />
              <stop offset="1" stopColor="#56d977" />
            </linearGradient>
          </defs>
          <path d="M24 10V7" fill="none" stroke="#26352c" strokeWidth="2.2" strokeLinecap="round" />
          <circle cx="24" cy="5.5" r="2.4" fill="#59df7b" stroke="#26352c" strokeWidth="1.3" />
          <path d="M17 30h14l3.1 8.2a2 2 0 0 1-1.9 2.7H15.8a2 2 0 0 1-1.9-2.7L17 30Z" fill="url(#robot-body)" stroke="#26352c" strokeWidth="1.4" strokeLinejoin="round" />
          <path d="M17 34h14" fill="none" stroke="#fff4b0" strokeWidth="1.2" strokeLinecap="round" />
          <rect x="9" y="10" width="30" height="23" rx="8" fill="url(#robot-shell)" stroke="#26352c" strokeWidth="1.5" />
          <path d="M13 15c3-2.7 7-3.3 11-3" fill="none" stroke="#fff" strokeOpacity=".8" strokeWidth="1.5" strokeLinecap="round" />
          <rect x="13" y="16" width="22" height="11" rx="5.5" fill="#17231c" />
          <circle cx="19.5" cy="21.5" r="2.4" fill="#fff6ce" />
          <circle cx="28.5" cy="21.5" r="2.4" fill="#fff6ce" />
          <circle cx="20" cy="21.5" r="1.2" fill="#36b85e" />
          <circle cx="29" cy="21.5" r="1.2" fill="#36b85e" />
          <path d="M21 29q3 2 6 0" fill="none" stroke="#eaffb2" strokeWidth="1.5" strokeLinecap="round" />
        </svg>
      </button>
    );
  }

  return (
    <div className={`chat-widget ${isMinimized ? "is-minimized" : ""} ${isMaximized ? "is-maximized" : ""}`}>
      <div className="chat-header">
        <div className="brand-mark" aria-label="MTN">MTN</div>
        <div className="chat-header-text">
          <div className="chat-title">YOLO Assistant</div>
          <div className="chat-status"><span /> Online · Here to help</div>
        </div>

        <div className="chat-window-controls">
          <button type="button" aria-label="Minimize chat" onClick={toggleMinimize}>
            −
          </button>
          <button type="button" aria-label="Maximize chat" onClick={toggleMaximize}>
            □
          </button>
          <button type="button" aria-label="Close chat" onClick={handleClose}>
            ×
          </button>
        </div>

        <button className="new-conversation-button" onClick={handleRestart} disabled={loading}>
          New chat
        </button>
      </div>

      {!isMinimized && (
        <>
          <div className="chat-messages">
            <div className="chat-welcome">Welcome to MTN YOLO. Choose an option below or ask a question.</div>
            {messages.map((msg, i) => (
              <div key={i} className={`chat-bubble ${msg.from}`}>
                <div className="chat-text">{msg.text}</div>
                {msg.buttons && msg.buttons.length > 0 && (
                  <div className="chat-buttons">
                    {msg.buttons.map((btn, j) => (
                      <button key={j} onClick={() => handleButtonClick(btn)} disabled={loading}>
                        <span className="menu-option-number">{btn.value}</span>
                        <span>{btn.label}</span>
                      </button>
                    ))}
                  </div>
                )}
                {msg.isEnd && (
                  <div className="chat-buttons">
                    <button onClick={handleRestart}>New conversation</button>
                  </div>
                )}
              </div>
            ))}
            {loading && <div className="chat-bubble bot chat-loading">…</div>}
            <div ref={bottomRef} />
          </div>

          <form className="chat-ask-bar" onSubmit={handleAskSubmit}>
            <input
              type="text"
              placeholder={expectsMenuText ? "Type your answer to continue…" : "Ask about YOLO, bundles, or Ihereze…"}
              value={freeText}
              onChange={(e) => setFreeText(e.target.value)}
              disabled={loading}
            />
            <button type="submit" disabled={loading}>{expectsMenuText ? "Send" : "Ask"}</button>
          </form>
        </>
      )}
    </div>
  );
}
