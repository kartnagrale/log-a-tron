import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
export interface ShipperDestination { id:string; name:string; endpoint:string; insecure:boolean }
export interface ShipperRevision { id:string; version:number; sha256:string; sourceCount:number; createdAt:string; validationStatus:string; validationMessage:string|null; yaml:string; warnings:string[] }
export interface ShipperDeployment { id:string; version:number; previousVersion:number|null; action:string; status:string; requestedAt:string; completedAt:string|null; error:string|null }
export interface ShipperHistory { agentId:string; serverId:string; selectedVersion:number|null; deploymentEnabled:boolean; deploymentAvailable:boolean; deploymentReason:string|null; protocol:string|null; lastIngestionAt:string|null; revisions:ShipperRevision[]; deployments:ShipperDeployment[] }
@Injectable({providedIn:'root'})
export class ShipperApiService {
 private readonly http=inject(HttpClient); private readonly base=environment.apiBaseUrl+'/v1/admin';
 destinations(server:string){return this.http.get<ShipperDestination[]>(this.base+'/servers/'+server+'/shipper-destinations');}
 history(agent:string){return this.http.get<ShipperHistory>(this.base+'/collector-agents/'+agent+'/shipper');}
 generate(agentId:string){return this.http.post<ShipperRevision>(this.base+'/shipper-configurations/generate',{agentId});}
 validate(agent:string,version:number){return this.http.post<ShipperRevision>(this.revisionUrl(agent,version)+'/validate',{});}
 download(agent:string,version:number){return this.http.get(this.revisionUrl(agent,version)+'/download',{responseType:'blob'});}
 settings(agent:string,enabled:boolean){return this.http.put<void>(this.base+'/collector-agents/'+agent+'/deployment-settings',{enabled,confirmed:true});}
 deploy(agent:string,version:number,expectedSelectedVersion:number|null,rollback=false){return this.http.post<ShipperDeployment>(this.base+'/collector-agents/'+agent+(rollback?'/rollback':'/deployments'),{version,expectedSelectedVersion,confirmed:true});}
 private revisionUrl(agent:string,version:number){return this.base+'/collector-agents/'+agent+'/config-revisions/'+version;}
}
