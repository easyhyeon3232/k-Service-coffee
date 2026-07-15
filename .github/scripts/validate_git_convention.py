import os
import re
import subprocess
import sys


ALLOWED_BRANCH_TYPES = ("feature", "fix", "refactor", "docs", "test", "chore")
ALLOWED_COMMIT_TYPES = ("feat", "fix", "refactor", "docs", "test", "chore")

BRANCH_PATTERN = re.compile(
    r"^(?:main|dev|(?:feature|fix|refactor|docs|test|chore)/[a-z0-9]+(?:-[a-z0-9]+)*)$"
)
COMMIT_PATTERN = re.compile(r"^(feat|fix|refactor|docs|test|chore): [^\s].*[^.\s]$")


def fail(messages: list[str]) -> int:
    print("Git convention validation failed.", file=sys.stderr)
    for message in messages:
        print(f"- {message}", file=sys.stderr)
    return 1


def get_env(name: str) -> str:
    value = os.getenv(name, "").strip()
    if not value:
        raise RuntimeError(f"Missing required environment variable: {name}")
    return value


def validate_branch_flow(base_ref: str, head_ref: str) -> list[str]:
    errors: list[str] = []

    if not BRANCH_PATTERN.match(head_ref):
        errors.append(
            "브랜치 이름은 main, dev 또는 "
            "`feature/...`, `fix/...`, `refactor/...`, `docs/...`, `test/...`, `chore/...` 형식을 따라야 합니다."
        )
        return errors

    if head_ref == "main":
        errors.append("PR source branch로 main은 사용할 수 없습니다.")
        return errors

    if head_ref == "dev":
        if base_ref != "main":
            errors.append("`dev` 브랜치는 `main`으로만 PR을 보내야 합니다.")
        return errors

    branch_type = head_ref.split("/", 1)[0]
    if branch_type in ALLOWED_BRANCH_TYPES and base_ref != "dev":
        errors.append(f"`{head_ref}` 브랜치는 `dev`로 PR을 보내야 합니다.")

    return errors


def get_commit_subjects(base_ref: str) -> list[str]:
    subprocess.run(
        ["git", "fetch", "--no-tags", "--depth=1", "origin", base_ref],
        capture_output=True,
        text=True,
        check=True,
    )

    merge_base = subprocess.run(
        ["git", "merge-base", f"origin/{base_ref}", "HEAD"],
        capture_output=True,
        text=True,
        check=True,
    ).stdout.strip()

    log_output = subprocess.run(
        ["git", "log", "--format=%s", f"{merge_base}..HEAD"],
        capture_output=True,
        text=True,
        check=True,
    ).stdout

    return [line.strip() for line in log_output.splitlines() if line.strip()]


def validate_commit_messages(commit_subjects: list[str]) -> list[str]:
    errors: list[str] = []

    if not commit_subjects:
        errors.append("PR에서 검사할 커밋 메시지를 찾지 못했습니다.")
        return errors

    for subject in commit_subjects:
        if subject.startswith("Merge ") or subject.startswith("Revert "):
            continue

        if not COMMIT_PATTERN.match(subject):
            errors.append(
                "커밋 메시지는 `type: 변경 내용` 형식이어야 하고 "
                f"type은 {', '.join(ALLOWED_COMMIT_TYPES)} 만 허용됩니다: `{subject}`"
            )

    return errors


def main() -> int:
    try:
        base_ref = get_env("PR_BASE_REF")
        head_ref = get_env("PR_HEAD_REF")

        errors = validate_branch_flow(base_ref, head_ref)
        errors.extend(validate_commit_messages(get_commit_subjects(base_ref)))

        if errors:
            return fail(errors)

        print("Git convention validation passed.")
        return 0
    except subprocess.CalledProcessError as error:
        if error.stderr:
            print(error.stderr, file=sys.stderr)
        print(str(error), file=sys.stderr)
        return 1
    except Exception as error:
        print(str(error), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
