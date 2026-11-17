import React, { useEffect, useState } from 'react';
export default function Scanner({ onScan }: { onScan: (s: string) => void }) {
  const [message, setMessage] = useState(''),
    [active, setActive] = useState(false);
  const video = React.useRef<HTMLVideoElement>(null);
  useEffect(() => {
    if (!active) return;
    let cancelled = false,
      stream: MediaStream | undefined,
      timer: ReturnType<typeof setTimeout>;
    const start = async () => {
      try {
        const Detector = (window as any).BarcodeDetector;
        if (!Detector)
          throw Error(
            'Dieser Browser unterstützt keinen Kamerascan. Bitte den Reservierungslink eingeben.',
          );
        const detector = new Detector({ formats: ['qr_code'] });
        stream = await navigator.mediaDevices.getUserMedia({
          video: { facingMode: 'environment' },
        });
        if (cancelled) {
          stream.getTracks().forEach((t) => t.stop());
          return;
        }
        video.current!.srcObject = stream;
        await video.current!.play();
        const scan = async () => {
          if (cancelled) return;
          const results = await detector.detect(video.current);
          const match = results[0]?.rawValue.match(/guest=([a-f0-9]{64})/);
          if (match) {
            onScan(match[1]);
            setActive(false);
          } else
            timer = setTimeout(
              () =>
                void scan().catch((e) => {
                  setMessage(e.message);
                  setActive(false);
                }),
              300,
            );
        };
        await scan();
      } catch (e) {
        setMessage((e as Error).message);
        setActive(false);
      }
    };
    void start();
    return () => {
      cancelled = true;
      clearTimeout(timer);
      stream?.getTracks().forEach((t) => t.stop());
    };
  }, [active]);
  return (
    <section>
      <button onClick={() => setActive(!active)}>
        {active ? 'Kamera schließen' : 'QR-Code mit Kamera scannen'}
      </button>
      {message && <p>{message}</p>}
      {active && <video ref={video} muted playsInline />}
    </section>
  );
}
