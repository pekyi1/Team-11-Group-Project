export default function Home() {
  return (
    <main style={{ padding: "2rem", fontFamily: "sans-serif" }}>
      <h1>ServiceHub</h1>
      <p>Internal Service Request &amp; Ticketing System</p>
      <p>
        API:{" "}
        <code>{process.env.NEXT_PUBLIC_API_URL ?? "not configured"}</code>
      </p>
    </main>
  );
}
