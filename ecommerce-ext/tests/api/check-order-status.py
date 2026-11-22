"""Validates the guest order-status answer: allowed keys only, no personal fields."""
import json
import re
import sys

body, order_id = sys.argv[1], sys.argv[2]
d = json.load(open(body))
top = {"orderId", "status", "updatedAt", "createdAt", "total", "items"}
line = {"name", "quantity", "unitPrice", "lineTotal"}
personal = re.compile("address|phone|email|recipient|customer|user", re.I)
ok = d.get("status") == "PAID" and str(d.get("orderId")) == order_id and set(d) == top
ok = ok and isinstance(d["items"], list) and len(d["items"]) > 0
ok = ok and all(set(i) == line for i in d["items"])
ok = ok and not any(personal.search(k) for k in d)
ok = ok and not any(personal.search(k) for i in d["items"] for k in i)
sys.exit(0 if ok else 1)
