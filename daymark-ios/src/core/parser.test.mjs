import test from 'node:test';
import assert from 'node:assert/strict';
import { parseNaturalTask } from './parser.ts';

test('parseNaturalTask: speech engine pm normalization and time parsing', () => {
  const result = parseNaturalTask('call mom at 6 pm');
  assert.equal(result.cleanTitle, 'Call mom');
  assert.equal(result.dueTimeMinutes, 18 * 60);
  assert.ok(result.tokens.includes('6:00 PM'));
});

test('parseNaturalTask: trailing p.m. with period', () => {
  const result = parseNaturalTask('call mom at 6 p.m.');
  assert.equal(result.cleanTitle, 'Call mom');
  assert.equal(result.dueTimeMinutes, 18 * 60);
  assert.ok(result.tokens.includes('6:00 PM'));
});

test('parseNaturalTask: priority tag !urgent', () => {
  const result = parseNaturalTask('prepare presentation !urgent');
  assert.equal(result.cleanTitle, 'Prepare presentation');
  assert.equal(result.priority, 4);
});

test('parseNaturalTask: spoken urgent priority', () => {
  const result = parseNaturalTask('doctor appointment urgent');
  assert.equal(result.cleanTitle, 'Doctor appointment');
  assert.equal(result.priority, 4);
});

test('parseNaturalTask: category hash tag #work', () => {
  const result = parseNaturalTask('send quarterly update #work');
  assert.equal(result.cleanTitle, 'Send quarterly update');
  assert.equal(result.categoryName, 'work');
});

test('parseNaturalTask: relative date tomorrow', () => {
  const result = parseNaturalTask('buy groceries tomorrow');
  assert.equal(result.cleanTitle, 'Buy groceries');
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  assert.equal(result.dueDate, today.getTime() + 86400000);
});

test('parseNaturalTask: action prefixes stripped cleanly', () => {
  const result = parseNaturalTask('remind me to water plants tomorrow');
  assert.equal(result.cleanTitle, 'Water plants');
});
