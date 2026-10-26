import { AfterViewInit, Directive, ElementRef, EventEmitter, HostListener, OnDestroy, Output, inject } from '@angular/core';

/** Keyboard containment and focus restoration for operational detail panels. */
@Directive({ selector: '[appDetailPanel]', standalone: true, host: { role: 'dialog', 'aria-modal': 'true', tabindex: '-1' } })
export class DetailPanelDirective implements AfterViewInit, OnDestroy {
  @Output() readonly panelClose = new EventEmitter<void>();
  private readonly element: HTMLElement = inject(ElementRef).nativeElement;
  private readonly previous = document.activeElement as HTMLElement | null;
  ngAfterViewInit() { queueMicrotask(() => this.element.querySelector<HTMLElement>('button')?.focus()); }
  ngOnDestroy() { if (this.previous?.isConnected) this.previous.focus(); }
  @HostListener('keydown', ['$event']) onKeydown(event: KeyboardEvent) {
    if (event.key === 'Escape') { event.preventDefault(); this.panelClose.emit(); }
    if (event.key !== 'Tab') return;
    const items = Array.from(this.element.querySelectorAll<HTMLElement>('button:not(:disabled), a[href], input:not(:disabled), select:not(:disabled), summary, [tabindex="0"]')).filter(item => item.getClientRects().length);
    const first = items[0], last = items.at(-1);
    if (!first) { event.preventDefault(); this.element.focus(); return; }
    if (event.shiftKey && (document.activeElement === first || document.activeElement === this.element)) { event.preventDefault(); last?.focus(); }
    else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
  }
}
