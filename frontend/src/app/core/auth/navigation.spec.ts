import { CAPABILITY, Capability } from './capability.service';
import { visibleNavigation } from './navigation';

describe('visibleNavigation',()=>{
 it('shows administration only when its capability is present',()=>{const operational=new Set<Capability>([CAPABILITY.VIEW_OVERVIEW,CAPABILITY.SEARCH_LOGS,CAPABILITY.VIEW_TRACES,CAPABILITY.RUN_INVESTIGATION,CAPABILITY.VIEW_CATALOG]);const labels=visibleNavigation(operational).flatMap(group=>group.items.map(item=>item.label));expect(labels).toContain('Projects');expect(labels).not.toContain('Administration');operational.add(CAPABILITY.VIEW_ADMINISTRATION);expect(visibleNavigation(operational).flatMap(group=>group.items.map(item=>item.label))).toContain('Administration');});
 it('omits empty groups',()=>expect(visibleNavigation(new Set<Capability>())).toEqual([]));
});
