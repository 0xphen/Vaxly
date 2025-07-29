# 🌳 PebbleTree: A High-Performance, Zero-Copy Key-Value Store in Rust

PebbleTree is a fast, durable, and memory-efficient storage engine in Rust. It provides a key-value interface and is inspired by the LSM-tree architecture used in systems like RocksDB and LevelDB - it’s built to prioritize:

* **High write throughput** using an append-only architecture
* **Crash safety** with a robust write-ahead log (WAL)
* **Efficient disk usage** through smart flushing and compaction

PebbleTree follows a **zero-copy approach**, using Rust’s low-level control to minimize unnecessary memory allocations and data copies. That means direct byte-slice manipulation, efficient memory layout, and fewer bottlenecks — ideal for systems where performance matters.

---

## 🛠️ What You'll Find Inside

PebbleTree is a low-level key-value store built to explore the core mechanics of storage engines — from logging to compaction. It includes:

* **Write-Ahead Logging (WAL):** Ensures durability by recording writes before applying them.
* **Memtables & SSTables:** In-memory buffers and on-disk sorted files, just like production-grade databases.
* **Flush & Compaction Pipelines:** Moves data efficiently from memory to disk and keeps it optimized for reads.
* **Zero-Copy I/O & Memory Layout Tuning:** Makes the most of Rust’s strengths for high performance.

Whether you're exploring how storage engines work, leveling up your systems programming skills, or just need a lightweight key-value store, PebbleTree is built to be both a practical tool and a learning resource.

---

## 🏗️ Architecture

A high-level overview of PebbleTree's components and data flow:

* **Writes** go to the **Write-Ahead Log (WAL)** for durability, then to an in-memory **Memtable**.
* Full Memtables are **flushed** to new, immutable **Sorted String Tables (SSTables)** on disk.
* **Reads** first check the Memtable, then search through SSTables (newest to oldest).
* A background **compaction** process merges and optimizes SSTables, reclaiming space and improving read performance over time.

---
