import { useEffect, useRef } from 'react'

export default function TerminalLog({ lines = [], className = '' }) {
  const bottomRef = useRef(null)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [lines])

  return (
    <div
      className={`bg-gray-900 text-green-400 font-mono text-xs rounded overflow-y-auto p-4 ${className}`}
      style={{ minHeight: '300px', maxHeight: '500px' }}
    >
      {lines.length === 0 ? (
        <span className="text-gray-500">Waiting for logs…</span>
      ) : (
        lines.map((line, i) => (
          <div key={i} className="leading-5 whitespace-pre-wrap break-all">
            <span className="text-gray-500 select-none mr-2">{String(i + 1).padStart(4, ' ')}</span>
            {line}
          </div>
        ))
      )}
      <div ref={bottomRef} />
    </div>
  )
}
