function PortfolioFooter() {
  return (
    <footer className="border-t border-slate-700 bg-slate-900">
      <div className="mx-auto flex max-w-6xl flex-col items-start justify-between gap-4 px-4 py-8 sm:flex-row sm:items-center">
        {/* Copyright & Contact Section */}
        <div className="text-slate-300">
          <p>© 2026 Gurupada Nayak. Made with ♥ in India</p>
          <p className="mt-1">
            For institute signup & queries:{' '}
            <a
              href="mailto:vidyapeeth.in@gmail.com"
              className="underline hover:text-white"
            >
              vidyapeeth.in@gmail.com
            </a>
          </p>
        </div>

        {/* Credit Section */}
        <a
          href="https://gurupadanayak.in/?ref=vidyapeeth"
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
