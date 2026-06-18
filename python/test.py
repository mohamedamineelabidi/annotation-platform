import json
from pathlib import Path


def main():
    metrics_path = Path("python/metrics.json")
    if not metrics_path.exists():
        print("No metrics found. Run train.py first.")
        return
    metrics = json.loads(metrics_path.read_text(encoding="utf-8"))
    print(f"Test accuracy: {metrics.get('accuracy')}")
    print(f"Test f1Score: {metrics.get('f1Score')}")


if __name__ == "__main__":
    main()
