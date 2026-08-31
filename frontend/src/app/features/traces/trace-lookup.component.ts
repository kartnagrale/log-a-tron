import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

@Component({standalone:true,imports:[FormsModule],template:`
  <header class="page-header"><div><p class="eyebrow">DISTRIBUTED TRACING</p><h1>Trace lookup</h1><p>Open a trace by its authorized 32-character identifier, or pivot here from a log event.</p></div></header>
  <section class="panel trace-lookup">
    <span class="trace-lookup-icon"><i class="pi pi-share-alt"></i></span>
    <div><label for="trace-id">Trace ID</label><input id="trace-id" maxlength="32" [(ngModel)]="traceId" placeholder="52b11701038c5605a9c374965e4e8d8c" (keydown.enter)="open()"><small>Existence is disclosed only after backend scope verification.</small></div>
    <button class="primary" (click)="open()" [disabled]="traceId.trim().length!==32">Open trace</button>
  </section>
`})
export class TraceLookupComponent { private readonly router=inject(Router);traceId='';open(){const id=this.traceId.trim();if(id.length===32)void this.router.navigate(['/traces',id]);} }
