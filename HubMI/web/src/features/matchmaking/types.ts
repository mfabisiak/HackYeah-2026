export interface MatchedInnovation {
  id: string
  title: string
  summary: string
  areas: string[]
  targetGroups: string[]
  stage: string
}

export interface MatchItem {
  innovation: MatchedInnovation
  score: number
  reasons: string[]
  matchedTerms: string[]
}

export interface SimilarNeedItem {
  id: string
  excerpt: string
  area?: string | null
}

export interface MatchmakingResultData {
  needId: string
  matches: MatchItem[]
  similarNeeds: SimilarNeedItem[]
  noGoodMatch: boolean
}
