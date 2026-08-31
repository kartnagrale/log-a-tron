import { CAPABILITY, Capability } from './capability.service';

export interface NavigationItem { label:string; route:string; icon:string; capability:Capability; exact?:boolean }
export interface NavigationGroup { label:string; items:NavigationItem[] }

export const NAVIGATION: NavigationGroup[] = [
  {label:'Workspace',items:[
    {label:'Overview',route:'/overview',icon:'pi-chart-bar',capability:CAPABILITY.VIEW_OVERVIEW},
    {label:'Log Explorer',route:'/logs',icon:'pi-align-left',capability:CAPABILITY.SEARCH_LOGS},
    {label:'Traces',route:'/traces',icon:'pi-share-alt',capability:CAPABILITY.VIEW_TRACES},
    {label:'Investigations',route:'/investigations',icon:'pi-sparkles',capability:CAPABILITY.RUN_INVESTIGATION}
  ]},
  {label:'Platform',items:[
    {label:'Projects',route:'/projects',icon:'pi-sitemap',capability:CAPABILITY.VIEW_CATALOG},
    {label:'Administration',route:'/administration',icon:'pi-sliders-h',capability:CAPABILITY.VIEW_ADMINISTRATION}
  ]}
];

export function visibleNavigation(capabilities: ReadonlySet<Capability>): NavigationGroup[] {
  return NAVIGATION.map(group => ({...group,items:group.items.filter(item => capabilities.has(item.capability))})).filter(group => group.items.length > 0);
}
