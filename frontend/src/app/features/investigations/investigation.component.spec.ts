import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { LogQueryApiService } from '../../core/api/log-query-api.service';
import { EvidenceStatus, InvestigationResult } from '../../core/models/api.models';
import { InvestigationComponent } from './investigation.component';

function result(confidence: EvidenceStatus): InvestigationResult {
  return {
    id: 'test-investigation',
    query: { projectId: 'authorized-project', environmentId: null, from: '', to: '', identifierType: 'TRACE_ID', identifierValue: null },
    deterministic: { summary: 'Bounded assessment', confidence, timeline: [], evidence: [], exceptionPatterns: [], dependencyFlow: [], affectedServices: [], affectedHosts: [], firstFailureEvidenceId: null, downstreamSymptoms: [], qualityWarnings: [] },
    ai: { rca: null, provider: 'deterministic-stub', model: 'grounded-template-v1', latencyMs: 0, outcome: 'SUCCESS' }
  };
}

describe('Investigation confidence presentation', () => {
  function setup() {
    TestBed.configureTestingModule({ providers: [
      { provide: LogQueryApiService, useValue: {} },
      { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap({}) } } }
    ] });
    return TestBed.runInInjectionContext(() => new InvestigationComponent());
  }

  for (const [confidence, title] of [
    ['CONFIRMED_FACT', 'Root-cause finding'],
    ['STRONG_EVIDENCE', 'Supported root-cause candidate'],
    ['PROBABLE_INFERENCE', 'Probable root cause'],
    ['POSSIBLE_HYPOTHESIS', 'Root-cause hypothesis']
  ] as const) {
    it(`preserves the ${confidence} classification in the finding label`, () => {
      const component = setup(), assessment = result(confidence);
      expect(component.findingConfidence(assessment)).toBe(confidence);
      expect(component.rootCauseLabel(assessment)).toBe(title);
      expect(component.firstFailure(assessment)).toBeUndefined();
    });
  }

  it('uses the confidence of the provider assessment displayed as the root cause', () => {
    const component = setup(), assessment = result('CONFIRMED_FACT');
    assessment.ai.rca = { summary: '', mostLikelyRootCause: 'Candidate explanation', confidence: 'PROBABLE_INFERENCE', evidenceReferences: [], timeline: [], affectedServices: [], downstreamSymptoms: [], whyThisLikelyHappened: '', suggestedInvestigationSteps: [], possibleRemediation: [], uncertainties: [] };
    expect(component.rootCause(assessment)).toBe('Candidate explanation');
    expect(component.rootCauseLabel(assessment)).toBe('Probable root cause');
    expect(assessment.deterministic.confidence).toBe('CONFIRMED_FACT');
  });
});
