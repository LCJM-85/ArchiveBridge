# -*- coding: utf-8 -*-
import os
import threading
import psycopg2
from psycopg2 import pool

DB_CONFIG = {
    "host": os.getenv("DB_HOST", "localhost"),
    "port": int(os.getenv("DB_PORT", "5432")),
    "dbname": os.getenv("DB_NAME", "scau_archive"),
    "user": os.getenv("DB_USER", "postgres"),
    "password": os.getenv("DB_PASS", "123456"),
}

_pool = None
_pool_lock = threading.Lock()


def _get_pool():
    global _pool
    if _pool is None:
        with _pool_lock:
            if _pool is None:
                _pool = pool.SimpleConnectionPool(1, 10, **DB_CONFIG)
    return _pool


def get_conn():
    return _get_pool().getconn()


def put_conn(conn):
    if conn:
        try:
            _get_pool().putconn(conn)
        except Exception:
            conn.close()
