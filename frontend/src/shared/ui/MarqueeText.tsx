"use client";

import { type CSSProperties, type ReactNode, useEffect, useRef, useState } from "react";
import { joinClassNames } from "@/shared/lib";

interface IMarqueeTextProps {
  children: ReactNode;
  className?: string;
}

const END_OVERSHOOT_PIXELS = 4;
const MIN_DURATION_SECONDS = 6;
const PIXELS_PER_SECOND = 5;

export const MarqueeText = ({ children, className }: IMarqueeTextProps) => {
  const containerRef = useRef<HTMLSpanElement>(null);
  const contentRef = useRef<HTMLSpanElement>(null);
  const [distance, setDistance] = useState(0);

  useEffect(() => {
    const container = containerRef.current;
    const content = contentRef.current;
    if (!container || !content) return;

    const measure = () => setDistance(Math.max(0, content.scrollWidth - container.clientWidth));
    measure();

    const observer = new ResizeObserver(measure);
    observer.observe(container);
    observer.observe(content);

    return () => observer.disconnect();
  }, []);

  const travel = distance > 0 ? distance + END_OVERSHOOT_PIXELS : 0;
  const style = {
    "--marquee-distance": `${travel}px`,
    "--marquee-duration": `${Math.max(MIN_DURATION_SECONDS, Math.round(travel / PIXELS_PER_SECOND))}s`,
    animation: travel > 0 ? "marquee var(--marquee-duration) ease-in-out infinite" : undefined,
  } as CSSProperties;

  return (
    <span ref={containerRef} className={joinClassNames("marquee block overflow-hidden", className)}>
      <span ref={contentRef} style={style} className="marquee-content inline-block whitespace-nowrap">
        {children}
      </span>
    </span>
  );
};
