import ChatWidget from "./ChatWidget";

function App() {
  return (
    <div style={{ minHeight: "100vh", background: "radial-gradient(circle at top, #3b3b3b, #101010 65%)", display: "flex", alignItems: "center", padding: "24px" }}>
      <ChatWidget />
    </div>
  );
}

export default App;
