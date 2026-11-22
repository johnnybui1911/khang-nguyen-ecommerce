#!/usr/bin/env node
/**
 * Environment Variable Loader for the ak-journal skill.
 *
 * Vendored (not shared) — this skill installs standalone and cannot import
 * across the source `kits/` tree once installed, so it carries its own copy
 * of the env-cascade loader. Mirrors the ak-seo skill's env-loader.cjs
 * pattern (see references/env-cascade.md for rationale).
 *
 * Cascade (highest → lowest priority):
 *   1. process.env                  (already-set values always win)
 *   2. <cwd>/.agentkit/.env         (project-scoped, preferred)
 *   3. $AGENTKIT_HOME/.env          (user-scoped, preferred; defaults to
 *                                    ~/.agentkit/.env when AGENTKIT_HOME
 *                                    is unset or empty; skipped when it is
 *                                    not an absolute path)
 *   4. <cwd>/.claude/.env           (legacy project-scoped)
 *   5. ~/.claude/.env               (legacy user-scoped)
 *
 * Usage:
 *   const { loadEnv, parseEnvContent } = require('./env-loader.cjs');
 *   const env = loadEnv(process.cwd());
 *   const apiKey = env.ZERNIO_API_KEY;
 */

const fs = require('fs');
const path = require('path');
const os = require('os');

/**
 * Parse .env-formatted text into a plain key/value object.
 * Supports: comments (#...), blank lines, unquoted values, single- and
 * double-quoted values, `KEY=value=with=equals`, and `\n`/`\t` escapes
 * inside double-quoted values only (matches ak-seo's env-loader.cjs).
 *
 * @param {string} content
 * @returns {Record<string, string>}
 */
function parseEnvContent(content) {
  const env = {};
  if (typeof content !== 'string') return env;

  const lines = content.split('\n');
  for (const rawLine of lines) {
    const trimmed = rawLine.trim();
    if (!trimmed || trimmed.startsWith('#')) continue;

    const eqIndex = trimmed.indexOf('=');
    if (eqIndex === -1) continue;

    const key = trimmed.slice(0, eqIndex).trim();
    if (!key) continue;

    const rawValue = trimmed.slice(eqIndex + 1).trim();
    let value = rawValue;

    const isDoubleQuoted = rawValue.startsWith('"') && rawValue.endsWith('"') && rawValue.length >= 2;
    const isSingleQuoted = rawValue.startsWith("'") && rawValue.endsWith("'") && rawValue.length >= 2;

    if (isDoubleQuoted || isSingleQuoted) {
      value = rawValue.slice(1, -1);
    }

    if (isDoubleQuoted) {
      value = value.replace(/\\n/g, '\n').replace(/\\t/g, '\t');
    }

    env[key] = value;
  }

  return env;
}

/**
 * Safely read + parse an .env file. Never throws.
 * @param {string} filePath
 * @returns {Record<string, string>}
 */
function readEnvFile(filePath) {
  try {
    if (fs.existsSync(filePath) && fs.statSync(filePath).isFile()) {
      return parseEnvContent(fs.readFileSync(filePath, 'utf8'));
    }
  } catch (error) {
    if (process.env.DEBUG) {
      console.error(`[ak-journal env-loader] could not read ${filePath}: ${error.message}`);
    }
  }
  return {};
}

function getHomeDir() {
  return process.env.HOME || process.env.USERPROFILE || os.homedir();
}

let warnedRelativeHome = false;

/**
 * The AgentKit user home: $AGENTKIT_HOME when set, otherwise
 * <os-home>/.agentkit. Like the `ak` CLI, a non-absolute AGENTKIT_HOME is
 * refused (returns null, with one stderr warning per process): it would
 * resolve against the working directory and let an untrusted project supply
 * user-scope values.
 * @returns {string | null}
 */
function getAgentkitHome() {
  const configured = process.env.AGENTKIT_HOME;
  if (!configured) return path.join(getHomeDir(), '.agentkit');
  if (!path.isAbsolute(configured)) {
    if (!warnedRelativeHome) {
      warnedRelativeHome = true;
      console.error(
        '[ak-journal] AGENTKIT_HOME is not an absolute path, so the user-scoped ' +
          '$AGENTKIT_HOME/.env and config.yaml are skipped. Set AGENTKIT_HOME to an ' +
          'absolute path, or unset it to use ~/.agentkit.'
      );
    }
    return null;
  }
  return configured;
}

/**
 * Load the merged env cascade for a given project directory. Does NOT
 * mutate process.env — callers read from the returned object, falling back
 * to process.env themselves (or use {@link resolveEnv}).
 *
 * @param {string} [cwd] project root to resolve `.agentkit/.env` /
 *   `.claude/.env` from. Defaults to process.cwd().
 * @returns {Record<string, string>} merged file-sourced env (process.env
 *   NOT included — see resolveEnv for the process.env-aware accessor)
 */
function loadEnv(cwd) {
  const projectRoot = cwd || process.cwd();
  const homeDir = getHomeDir();
  const agentkitHome = getAgentkitHome();

  // Lowest priority first; later entries override earlier ones.
  const layers = [
    readEnvFile(path.join(homeDir, '.claude', '.env')), // 5. legacy user
    readEnvFile(path.join(projectRoot, '.claude', '.env')), // 4. legacy project
    agentkitHome ? readEnvFile(path.join(agentkitHome, '.env')) : {}, // 3. user
    readEnvFile(path.join(projectRoot, '.agentkit', '.env')), // 2. project
  ];

  let merged = {};
  for (const layer of layers) {
    merged = { ...merged, ...layer };
  }
  return merged;
}

/**
 * Resolve a single env var honoring the full cascade, process.env highest.
 * @param {string} key
 * @param {string} [cwd]
 * @param {string} [defaultValue]
 * @returns {string | undefined}
 */
function resolveEnv(key, cwd, defaultValue) {
  if (process.env[key] !== undefined) return process.env[key];
  const fileEnv = loadEnv(cwd);
  return fileEnv[key] !== undefined ? fileEnv[key] : defaultValue;
}

/**
 * Merge the cascade with process.env (process.env wins) into one object.
 * Convenience for callers that want a single lookup table.
 * @param {string} [cwd]
 * @returns {Record<string, string>}
 */
function resolveAllEnv(cwd) {
  return { ...loadEnv(cwd), ...process.env };
}

module.exports = {
  getAgentkitHome,
  loadEnv,
  parseEnvContent,
  resolveEnv,
  resolveAllEnv,
};
