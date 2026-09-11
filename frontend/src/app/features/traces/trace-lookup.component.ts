import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

@Component({standalone:true,imports:[FormsModule],template:`
  <header class="page-header"><div><p class="eyebrow">DISTRIBUTED TRACING</p><h1>Trace lookup</h1><p>Open an authorized distributed trace by its 32-character hexadecimal trace ID, or pivot here directly from a log event.</p></div></header>
  <section class="panel trace-lookup">
    <span class="trace-lookup-icon"><i class="pi pi-share-alt"></i></span>
    <div><label for="trace-id">Trace ID</label><input id="trace-id" maxlength="32" [(ngModel)]="traceId" placeholder="52b11701038c5605a9c374965e4e8d8c" (keydown.enter)="open()"><small>Trace existence is disclosed only after backend scope verification.</small></div>
    <button class="primary" (click)="open()" [disabled]="!valid()">Open trace</button>
  </section>
  <section class="panel trace-help">
    <div><p class="eyebrow">HOW TO USE IT</p><h2>Where does the trace ID come from?</h2></div>
    <div class="trace-help-grid">
      <article><i class="pi pi-search"></i><strong>1. From Log Explorer</strong><p>Open a log row that contains a traceId and choose <b>Open trace</b>. LOG-A-TRON carries the identifier into this screen automatically.</p></article>
      <article><i class="pi pi-sitemap"></i><strong>2. From an instrumented service</strong><p>The application must emit OpenTelemetry traces to the collector/Tempo. File logs alone cannot create a distributed trace.</p></article>
      <article><i class="pi pi-shield"></i><strong>3. Add project metadata</strong><p>Instrumented services should send <code>logatron.project.id</code> (or <code>project.id</code>) as an OpenTelemetry resource attribute so authorization can be enforced without relying on a matching log row.</p></article>
    </div>
  </section>
`})
export class TraceLookupComponent {
 private readonly router=inject(Router);traceId='';
 valid(){return /^[0-9a-fA-F]{32}$/.test(this.traceId.trim());}
 open(){const id=this.traceId.trim().toLowerCase();if(this.valid())void this.router.navigate(['/traces',id]);}
}
