function PortfolioFooter() {
  return (
    <footer className="border-t border-slate-700 bg-slate-900">
      <div className="mx-auto flex max-w-6xl flex-col items-start justify-between gap-4 px-4 py-8 sm:flex-row sm:items-center">
        {/* Copyright Section */}
        <p className="text-slate-300">
          © 2026{' '}
          <a
            href="https://gurupadanayak.in/?ref=vidyapeeth"
            target="_blank"
            rel="noopener noreferrer"
            aria-label="Visit Gurupada Nayak's portfolio website"
            className="text-slate-300 underline hover:text-white"
          >
            Gurupada Nayak
          </a>
          . Made with ♥ in India
        </p>

        {/* Credit Section */}
        <a
          href="https://gurupadanayak.in"
          target="_blank"
          rel="noopener noreferrer"
          aria-label="Visit GurupadaNayak's portfolio"
          className="text-slate-300 hover:text-white"
        >
          Crafted by <span className="text-amber-400">GurupadaNayak</span> →
        </a>
      </div>
    </footer>
  );
}

export default PortfolioFooter;
