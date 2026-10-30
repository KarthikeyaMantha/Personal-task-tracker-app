import type { ParsedTaskResult } from '../types';

export function parseNaturalTask(input: string): ParsedTaskResult {
  if (!input || !input.trim()) {
    return { cleanTitle: '', tokens: [] };
  }

  // 1. Normalize speech engine abbreviations
  let normalized = input
    .replace(/\bp\.m\.?(?=\s|$|[.,!?])/gi, 'pm')
    .replace(/\ba\.m\.?(?=\s|$|[.,!?])/gi, 'am')
    .replace(/\b(\d{1,2})\s*p\.m\.?(?=\s|$|[.,!?])/gi, '$1 pm')
    .replace(/\b(\d{1,2})\s*a\.m\.?(?=\s|$|[.,!?])/gi, '$1 am')
    .replace(/\b(\d{1,2})\s*pm\b/gi, '$1 pm')
    .replace(/\b(\d{1,2})\s*am\b/gi, '$1 am')
    .replace(/\bo'clock\b/gi, '')
    .trim();

  let workingTitle = normalized;
  const tokensToRemove: string[] = [];
  const detectedLabels: string[] = [];

  // 2. Extract Category (#work, #personal)
  let detectedCategoryName: string | undefined;
  const catMatch = /(?:^|\s)#([a-zA-Z0-9_\-]+)/i.exec(normalized);
  if (catMatch) {
    detectedCategoryName = catMatch[1];
    tokensToRemove.push(catMatch[0].trim());
    detectedLabels.push(`#${detectedCategoryName}`);
  }

  // 3. Extract Priority (!urgent, !high or spoken "urgent", "high priority", "important")
  let priority: number | undefined;
  const prioMatch = /(?:^|\s)!(urgent|high|med|medium|low|p1|p2|p3|p4)(?=\s|$)/i.exec(normalized);
  if (prioMatch) {
    const tag = prioMatch[1].toLowerCase();
    tokensToRemove.push(prioMatch[0].trim());
    if (tag === 'urgent' || tag === 'p4') priority = 4;
    else if (tag === 'high' || tag === 'p3') priority = 3;
    else if (tag === 'medium' || tag === 'med' || tag === 'p2') priority = 2;
    else priority = 1;
  } else {
    const spokenPrioMatch = /(?:^|\s)(?:priority\s+(urgent|high|medium|med|low)|(urgent|high\s+priority|important|low\s+priority))(?=\s|$)/i.exec(normalized);
    if (spokenPrioMatch) {
      tokensToRemove.push(spokenPrioMatch[0].trim());
      const p = (spokenPrioMatch[1] || spokenPrioMatch[2]).toLowerCase();
      if (p.includes('urgent') || p.includes('important')) priority = 4;
      else if (p.includes('high')) priority = 3;
      else if (p.includes('medium') || p.includes('med')) priority = 2;
      else priority = 1;
    }
  }

  if (priority) {
    detectedLabels.push(priority === 4 ? '!urgent' : priority === 3 ? '!high' : priority === 2 ? '!medium' : '!low');
  }

  // 4. Extract Time (at 6 pm, 5:30 pm, 17:00, at 5, this evening)
  let dueTimeMinutes: number | undefined;
  const time12Match = /(?:^|\s)(?:at\s+)?(\d{1,2})(?::(\d{2}))?\s*(am|pm)(?=\s|$|[.,!?])/i.exec(normalized);
  if (time12Match) {
    tokensToRemove.push(time12Match[0].trim());
    const rawHour = parseInt(time12Match[1], 10);
    const rawMinute = time12Match[2] ? parseInt(time12Match[2], 10) : 0;
    const isPm = time12Match[3].toLowerCase() === 'pm';
    let hour24 = rawHour % 12;
    if (isPm) hour24 += 12;
    dueTimeMinutes = hour24 * 60 + rawMinute;
    detectedLabels.push(`${rawHour === 0 ? 12 : rawHour}:${rawMinute.toString().padStart(2, '0')} ${isPm ? 'PM' : 'AM'}`);
  } else {
    const time24Match = /(?:^|\s)(?:at\s+)?([01]?\d|2[0-3]):([0-5]\d)(?=\s|$|[.,!?])/i.exec(normalized);
    if (time24Match) {
      tokensToRemove.push(time24Match[0].trim());
      const h = parseInt(time24Match[1], 10);
      const m = parseInt(time24Match[2], 10);
      dueTimeMinutes = h * 60 + m;
      detectedLabels.push(`${h}:${m.toString().padStart(2, '0')}`);
    } else {
      const timeAtMatch = /(?:^|\s)at\s+(\d{1,2})(?=\s|$|[.,!?])/i.exec(normalized);
      if (timeAtMatch) {
        tokensToRemove.push(timeAtMatch[0].trim());
        const h = parseInt(timeAtMatch[1], 10);
        const hour24 = h <= 7 ? h + 12 : h; // 1-7 implies PM
        dueTimeMinutes = hour24 * 60;
        detectedLabels.push(`${hour24 > 12 ? hour24 - 12 : hour24}:00 ${hour24 >= 12 ? 'PM' : 'AM'}`);
      }
    }
  }

  // 5. Extract Relative Date
  let dueDate: number | undefined;
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const startOfToday = today.getTime();

  const dateRelMatch = /(?:^|\s)(today|tomorrow|day\s+after\s+tomorrow|next\s+week)(?=\s|$)/i.exec(normalized);
  if (dateRelMatch) {
    tokensToRemove.push(dateRelMatch[0].trim());
    const rel = dateRelMatch[1].toLowerCase();
    if (rel === 'today') dueDate = startOfToday;
    else if (rel === 'tomorrow') dueDate = startOfToday + 86400000;
    else if (rel === 'day after tomorrow') dueDate = startOfToday + 2 * 86400000;
    else if (rel === 'next week') dueDate = startOfToday + 7 * 86400000;
    detectedLabels.push(rel.charAt(0).toUpperCase() + rel.slice(1));
  }

  // If time specified without date, default to today
  if (dueTimeMinutes !== undefined && !dueDate) {
    dueDate = startOfToday;
  }

  // Clean matched tokens from title
  for (const token of tokensToRemove) {
    const esc = token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    workingTitle = workingTitle.replace(new RegExp(`(?:^|\\s)${esc}(?=\\s|$|[.,!?])`, 'gi'), ' ');
  }

  workingTitle = workingTitle
    .replace(/(?:^|\s)\b(at|pm|am)\b\s*$/gi, '')
    .replace(/^\s*\b(at|pm|am)\b(?:\s|$)/gi, '')
    .replace(/\s+/g, ' ')
    .trim();

  // Strip leading action prefix if spoken e.g. "remind me to...", "create task..."
  workingTitle = workingTitle
    .replace(/^(?:remind\s+me\s+to|add\s+task|create\s+task|schedule|new\s+task)\s+/i, '')
    .trim();

  const finalTitle = workingTitle.length > 0
    ? workingTitle.charAt(0).toUpperCase() + workingTitle.slice(1)
    : input.trim();

  return {
    cleanTitle: finalTitle,
    categoryName: detectedCategoryName,
    priority,
    dueDate,
    dueTimeMinutes,
    tokens: detectedLabels
  };
}

