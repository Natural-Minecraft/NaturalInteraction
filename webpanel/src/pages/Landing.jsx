export default function Landing({ loading, error }) {
  return (
    <div className="landing">
      <div className="landing-card">
        <div className="landing-logo">
          <span className="landing-icon">✦</span>
          <h1>NaturalInteraction</h1>
          <p className="landing-subtitle">Story Editor</p>
        </div>
        {loading ? (
          <div className="landing-status">
            <div className="spinner" />
            <span>Connecting to server...</span>
          </div>
        ) : error ? (
          <div className="landing-error">
            <p>{error}</p>
            <p className="text-muted">Ketik <code>/ni connect</code> di Minecraft untuk mendapatkan link baru.</p>
          </div>
        ) : null}
      </div>
    </div>
  );
}
