import { CAPABILITY, resolveCapabilities } from './capability.service';
import { MeResponse } from '../models/api.models';

const me=(role:string,environment='DEV'):MeResponse=>({user:{id:'u',username:'demo',displayName:'Demo',email:'demo@local.test'},roles:[role],projects:[{projectId:'p',projectName:'Demo Marketplace',roles:[role],environments:[{environmentId:'e',code:environment,name:environment}]}]});

describe('resolveCapabilities',()=>{
 it('grants scoped operational capabilities from effective /me access',()=>{const caps=resolveCapabilities(me('VIEWER','PROD'));expect(caps.has(CAPABILITY.SEARCH_LOGS)).toBe(true);expect(caps.has(CAPABILITY.RUN_INVESTIGATION)).toBe(true);expect(caps.has(CAPABILITY.VIEW_CATALOG)).toBe(true);expect(caps.has(CAPABILITY.ACCESS_PRODUCTION)).toBe(true);});
 it('limits administration to roles accepted by backend administration rules',()=>{expect(resolveCapabilities(me('ADMIN')).has(CAPABILITY.VIEW_ADMINISTRATION)).toBe(true);expect(resolveCapabilities(me('PROJECT_ADMIN')).has(CAPABILITY.VIEW_ADMINISTRATION)).toBe(true);expect(resolveCapabilities(me('DEVELOPER')).has(CAPABILITY.VIEW_ADMINISTRATION)).toBe(false);});
 it('does not invent audit access or capabilities without effective scope',()=>{expect(resolveCapabilities(me('AUDITOR')).has(CAPABILITY.VIEW_AUDIT)).toBe(false);expect(resolveCapabilities({...me('VIEWER'),projects:[]}).size).toBe(0);});
});
